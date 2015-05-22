package com.gentics.mesh.cli;

import static com.gentics.mesh.util.DeploymentUtils.deployAndWait;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.spi.cluster.impl.hazelcast.HazelcastClusterManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InvalidNameException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.data.model.MeshRoot;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.LanguageRoot;
import com.gentics.mesh.core.data.model.Project;
import com.gentics.mesh.core.data.model.ProjectRoot;
import com.gentics.mesh.core.data.model.auth.Group;
import com.gentics.mesh.core.data.model.auth.GroupRoot;
import com.gentics.mesh.core.data.model.auth.Role;
import com.gentics.mesh.core.data.model.auth.RoleRoot;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.model.auth.UserRoot;
import com.gentics.mesh.core.data.model.schema.ObjectSchema;
import com.gentics.mesh.core.data.model.schema.ObjectSchemaRoot;
import com.gentics.mesh.core.data.model.schema.propertytypes.PropertyType;
import com.gentics.mesh.core.data.model.schema.propertytypes.BasicPropertyTypeSchema;
import com.gentics.mesh.core.data.service.ObjectSchemaService;
import com.gentics.mesh.core.repository.MeshRootRepository;
import com.gentics.mesh.core.repository.GroupRepository;
import com.gentics.mesh.core.repository.LanguageRepository;
import com.gentics.mesh.core.repository.ObjectSchemaRepository;
import com.gentics.mesh.core.repository.ProjectRepository;
import com.gentics.mesh.core.repository.RoleRepository;
import com.gentics.mesh.core.repository.UserRepository;
import com.gentics.mesh.core.verticle.AdminVerticle;
import com.gentics.mesh.core.verticle.MeshNodeVerticle;
import com.gentics.mesh.core.verticle.GroupVerticle;
import com.gentics.mesh.core.verticle.ObjectSchemaVerticle;
import com.gentics.mesh.core.verticle.ProjectVerticle;
import com.gentics.mesh.core.verticle.RoleVerticle;
import com.gentics.mesh.core.verticle.TagVerticle;
import com.gentics.mesh.core.verticle.UserVerticle;
import com.gentics.mesh.core.verticle.WebRootVerticle;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.MeshVerticleConfiguration;
import com.gentics.mesh.etc.LanguageEntry;
import com.gentics.mesh.etc.LanguageSet;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshConfiguration;

@Component
public class BootstrapInitializer {

	private static Logger log = LoggerFactory.getLogger(BootstrapInitializer.class);

	private Map<String, Class<? extends AbstractVerticle>> mandatoryVerticles = new HashMap<>();

	private MeshConfiguration configuration;

	@Autowired
	private MeshRootRepository rootRepository;

	@Autowired
	private ObjectSchemaRepository objectSchemaRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private LanguageRepository languageRepository;

	@Autowired
	private ObjectSchemaService objectSchemaService;

	@Autowired
	private MeshSpringConfiguration springConfiguration;

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Autowired
	private GraphDatabaseService graphDb;

	@Autowired
	private RouterStorage routerStorage;

	public BootstrapInitializer() {
		addMandatoryVerticle(UserVerticle.class);
		addMandatoryVerticle(GroupVerticle.class);
		addMandatoryVerticle(RoleVerticle.class);

		addMandatoryVerticle(TagVerticle.class);
		addMandatoryVerticle(MeshNodeVerticle.class);
		addMandatoryVerticle(WebRootVerticle.class);

		addMandatoryVerticle(ProjectVerticle.class);
		addMandatoryVerticle(ObjectSchemaVerticle.class);
		// addMandatoryVerticle(SearchVerticle.class);
		// addMandatoryVerticle(AuthenticationVerticle.class);
		addMandatoryVerticle(AdminVerticle.class);

	}

	private void addMandatoryVerticle(Class<? extends AbstractVerticle> clazz) {
		mandatoryVerticles.put(clazz.getSimpleName(), clazz);
	}

	/**
	 * Load verticles that are configured within the mesh configuration.
	 * 
	 * @throws InterruptedException
	 */
	private void loadConfiguredVerticles() throws InterruptedException {
		JsonObject defaultConfig = new JsonObject();
		defaultConfig.put("port", configuration.getHttpPort());

		for (Class<? extends AbstractVerticle> clazz : getMandatoryVerticleClasses().values()) {
			try {
				log.info("Loading mandatory verticle {" + clazz.getName() + "}.");
				// TODO handle custom config? i assume we will not allow this
				deployAndWait(springConfiguration.vertx(), defaultConfig, clazz);
			} catch (InterruptedException e) {
				log.error("Could not load mandatory verticle {" + clazz.getSimpleName() + "}.", e);
			}
		}

		for (String verticleName : configuration.getVerticles().keySet()) {
			if (getMandatoryVerticleClasses().containsKey(verticleName)) {
				log.error("Can't configure mandatory verticles. Skipping configured verticle {" + verticleName + "}");
				continue;
			}
			MeshVerticleConfiguration verticleConf = configuration.getVerticles().get(verticleName);
			JsonObject mergedVerticleConfig = new JsonObject();
			if (verticleConf.getVerticleConfig() != null) {
				mergedVerticleConfig = verticleConf.getVerticleConfig().copy();
			}
			mergedVerticleConfig.put("port", configuration.getHttpPort());
			try {
				log.info("Loading configured verticle {" + verticleName + "}.");
				deployAndWait(springConfiguration.vertx(), mergedVerticleConfig, verticleName);
			} catch (InterruptedException e) {
				log.error("Could not load verticle {" + verticleName + "}.", e);
			}
		}
	}

	private Map<String, Class<? extends AbstractVerticle>> getMandatoryVerticleClasses() {
		return mandatoryVerticles;
	}

	/**
	 * Initialize mesh.
	 * 
	 * @param configuration
	 * @param verticleLoader
	 * @throws Exception
	 */
	// @Transactional
	public void init(MeshConfiguration configuration, MeshCustomLoader<Vertx> verticleLoader) throws Exception {
		this.configuration = configuration;
		if (configuration.isClusterMode()) {
			joinCluster();
		}

		try (Transaction tx = graphDb.beginTx()) {
			initMandatoryData();
			tx.success();
		}
		loadConfiguredVerticles();
		if (verticleLoader != null) {
			verticleLoader.apply(springConfiguration.vertx());
		}
		try (Transaction tx = graphDb.beginTx()) {
			initProjects();
			tx.success();
		}

	}

	/**
	 * The projects share various subrouters. This method will add the subrouters for all registered projects.
	 * 
	 * @throws InvalidNameException
	 */
	private void initProjects() throws InvalidNameException {
		for (Project project : projectRepository.findAll()) {
			routerStorage.addProjectRouter(project.getName());
			log.info("Initalized project {" + project.getName() + "}");
		}
	}

	/**
	 * Use the hazelcast cluster manager to join the cluster of mesh instances.
	 */
	private void joinCluster() {
		HazelcastClusterManager manager = new HazelcastClusterManager();
		manager.setVertx(springConfiguration.vertx());
		manager.join(rh -> {
			if (!rh.succeeded()) {
				log.error("Error while joining mesh cluster.", rh.cause());
			}
		});
	}

	/**
	 * Setup various mandatory data. This includes mandatory root nodes and the admin user, group.
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public void initMandatoryData() throws JsonParseException, JsonMappingException, IOException {

		LanguageRoot languageRoot = languageRepository.findRoot();
		if (languageRoot == null) {
			languageRoot = new LanguageRoot();
			languageRoot = neo4jTemplate.save(languageRoot);
			log.info("Stored language root node");
		}

		GroupRoot groupRoot = groupRepository.findRoot();
		if (groupRoot == null) {
			groupRoot = new GroupRoot();
			groupRoot = neo4jTemplate.save(groupRoot);
			log.info("Stored group root node");
		}
		UserRoot userRoot = userRepository.findRoot();
		if (userRoot == null) {
			userRoot = new UserRoot();
			userRoot = neo4jTemplate.save(userRoot);
			log.info("Stored user root node");
		}
		RoleRoot roleRoot = roleRepository.findRoot();
		if (roleRoot == null) {
			roleRoot = new RoleRoot();
			roleRoot = neo4jTemplate.save(roleRoot);
			log.info("Stored role root node");
		}
		ProjectRoot projectRoot = projectRepository.findRoot();
		if (projectRoot == null) {
			projectRoot = new ProjectRoot();
			projectRoot = neo4jTemplate.save(projectRoot);
			log.info("Stored project root node");
		}

		// Save the default object schemas

		// Content
		ObjectSchema contentSchema = objectSchemaRepository.findByName("content");
		if (contentSchema == null) {
			contentSchema = new ObjectSchema("content");
			contentSchema.setDescription("Default schema for contents");
			contentSchema.setDisplayName("Content");

			BasicPropertyTypeSchema nameProp = new BasicPropertyTypeSchema(ObjectSchema.NAME_KEYWORD, PropertyType.I18N_STRING);
			nameProp.setDisplayName("Name");
			nameProp.setDescription("The name of the content.");
			contentSchema.addPropertyTypeSchema(nameProp);

			BasicPropertyTypeSchema displayNameProp = new BasicPropertyTypeSchema(ObjectSchema.DISPLAY_NAME_KEYWORD, PropertyType.I18N_STRING);
			displayNameProp.setDisplayName("Display Name");
			displayNameProp.setDescription("The display name property of the content.");
			contentSchema.addPropertyTypeSchema(displayNameProp);

			BasicPropertyTypeSchema contentProp = new BasicPropertyTypeSchema(ObjectSchema.CONTENT_KEYWORD, PropertyType.I18N_STRING);
			contentProp.setDisplayName("Content");
			contentProp.setDescription("The main content html of the content.");
			contentSchema.addPropertyTypeSchema(contentProp);

			objectSchemaService.save(contentSchema);
		}

		// Folder
		ObjectSchema folderSchema = objectSchemaRepository.findByName("folder");
		if (folderSchema == null) {
			folderSchema = new ObjectSchema("folder");
			folderSchema.setDescription("Default schema for folders");
			folderSchema.setDisplayName("Folder");

			BasicPropertyTypeSchema nameProp = new BasicPropertyTypeSchema(ObjectSchema.NAME_KEYWORD, PropertyType.I18N_STRING);
			nameProp.setDisplayName("Name");
			nameProp.setDescription("The name of the folder.");
			folderSchema.addPropertyTypeSchema(nameProp);
			objectSchemaService.save(folderSchema);
		}

		// Binary content for images and other downloads
		ObjectSchema binarySchema = objectSchemaRepository.findByName("binary-content");
		if (binarySchema == null) {
			binarySchema = new ObjectSchema("binary-content");
			binarySchema.setDescription("Default schema for binary contents");
			binarySchema.setDisplayName("Binary Content");

			BasicPropertyTypeSchema nameProp = new BasicPropertyTypeSchema(ObjectSchema.NAME_KEYWORD, PropertyType.I18N_STRING);
			nameProp.setDisplayName("Name");
			nameProp.setDescription("The name of the content.");
			binarySchema.addPropertyTypeSchema(nameProp);

			BasicPropertyTypeSchema displayNameProp = new BasicPropertyTypeSchema(ObjectSchema.DISPLAY_NAME_KEYWORD, PropertyType.I18N_STRING);
			displayNameProp.setDisplayName("Display Name");
			displayNameProp.setDescription("The display name property of the content.");
			binarySchema.addPropertyTypeSchema(displayNameProp);

			BasicPropertyTypeSchema binaryContentProp = new BasicPropertyTypeSchema(ObjectSchema.CONTENT_KEYWORD, PropertyType.BINARY);
			binaryContentProp.setDisplayName("Binary content");
			binaryContentProp.setDescription("The binary content of the content");
			binarySchema.addPropertyTypeSchema(binaryContentProp);
			objectSchemaService.save(contentSchema);
		}

		// Tag schema
		ObjectSchema tagSchema = objectSchemaRepository.findByName("tag");
		if (tagSchema == null) {
			tagSchema = new ObjectSchema("tag");
			tagSchema.setDisplayName("Tag");
			tagSchema.setDescription("Default schema for tags");
			tagSchema.addPropertyTypeSchema(new BasicPropertyTypeSchema(ObjectSchema.NAME_KEYWORD, PropertyType.I18N_STRING));
			tagSchema.addPropertyTypeSchema(new BasicPropertyTypeSchema(ObjectSchema.DISPLAY_NAME_KEYWORD, PropertyType.I18N_STRING));
			tagSchema.addPropertyTypeSchema(new BasicPropertyTypeSchema(ObjectSchema.CONTENT_KEYWORD, PropertyType.I18N_STRING));
			objectSchemaService.save(tagSchema);
		}

		ObjectSchemaRoot objectSchemaRoot = objectSchemaRepository.findRoot();
		if (objectSchemaRoot == null) {
			objectSchemaRoot = new ObjectSchemaRoot();
			objectSchemaRoot.getSchemas().add(tagSchema);
			objectSchemaRoot.getSchemas().add(contentSchema);
			objectSchemaRoot.getSchemas().add(binarySchema);
			objectSchemaRoot = neo4jTemplate.save(objectSchemaRoot);
			log.info("Stored schema root node");
		}

		// Verify that the root node is existing
		MeshRoot rootNode = rootRepository.findRoot();
		if (rootNode == null) {
			rootNode = new MeshRoot();
			rootNode.setProjectRoot(projectRoot);
			rootNode.setGroupRoot(groupRoot);
			rootNode.setRoleRoot(roleRoot);
			rootNode.setLanguageRoot(languageRoot);
			rootNode.setObjectSchemaRoot(objectSchemaRoot);
			rootNode.setUserRoot(userRoot);
			rootRepository.save(rootNode);
			log.info("Stored mesh root node");
		}
		// Reload the node to get one with a valid uuid
		// TODO check whether this really works. I assume i would have to commit first.
		rootNode = rootRepository.findRoot();

		initLanguages(rootNode);

		// Verify that an admin user exists
		User adminUser = userRepository.findByUsername("admin");
		if (adminUser == null) {
			adminUser = new User("admin");
			System.out.println("Enter admin password:");
			// Scanner scanIn = new Scanner(System.in);
			// String pw = scanIn.nextLine();
			// TODO remove later on
			String pw = "finger";
			// scanIn.close();
			adminUser.setPasswordHash(springConfiguration.passwordEncoder().encode(pw));
			userRepository.save(adminUser);
			log.info("Stored admin user");
		}
		rootNode.getUsers().add(adminUser);
		rootRepository.save(rootNode);

		Group adminGroup = groupRepository.findByName("admin");
		if (adminGroup == null) {
			adminGroup = new Group("admin");
			adminGroup.getUsers().add(adminUser);
			groupRepository.save(adminGroup);
			log.info("Stored admin group");
		}
		rootNode.addGroup(adminGroup);
		rootRepository.save(rootNode);

		Role adminRole = roleRepository.findByName("admin");
		if (adminRole == null) {
			adminRole = new Role("admin");
			adminGroup.addRole(adminRole);
		}

	}

	protected void initLanguages(MeshRoot rootNode) throws JsonParseException, JsonMappingException, IOException {

		long start = System.currentTimeMillis();
		final String filename = "languages.json";
		final InputStream ins = getClass().getResourceAsStream("/" + filename);
		if (ins == null) {
			throw new NullPointerException("Languages could not be loaded from classpath file {" + filename + "}");
		}
		LanguageSet languageSet = new ObjectMapper().readValue(ins, LanguageSet.class);
		for (Map.Entry<String, LanguageEntry> entry : languageSet.entrySet()) {
			String languageTag = entry.getKey();
			String languageName = entry.getValue().getName();
			String languageNativeName = entry.getValue().getNativeName();
			Language language = languageRepository.findByName(languageName);
			if (language == null) {
				language = new Language(languageName, languageTag);
				language.setNativeName(languageNativeName);
				rootNode.addLanguage(languageRepository.save(language));
				log.debug("Saved language {" + languageTag + " / " + languageName + "}");
				rootRepository.save(rootNode);
			}
		}
		long diff = System.currentTimeMillis() - start;
		log.info("Handling languages took: " + diff + " [ms]");
	}
}
