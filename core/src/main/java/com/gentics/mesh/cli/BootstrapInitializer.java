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
import java.util.NoSuchElementException;

import javax.naming.InvalidNameException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.data.model.root.GroupRoot;
import com.gentics.mesh.core.data.model.root.LanguageRoot;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.root.ProjectRoot;
import com.gentics.mesh.core.data.model.root.RoleRoot;
import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.model.root.UserRoot;
import com.gentics.mesh.core.data.model.schema.propertytype.BasicPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytype.PropertyType;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.MeshRootService;
import com.gentics.mesh.core.data.service.ProjectService;
import com.gentics.mesh.core.data.service.RoleService;
import com.gentics.mesh.core.data.service.SchemaService;
import com.gentics.mesh.core.data.service.UserService;
import com.gentics.mesh.core.verticle.AdminVerticle;
import com.gentics.mesh.core.verticle.GroupVerticle;
import com.gentics.mesh.core.verticle.MeshNodeVerticle;
import com.gentics.mesh.core.verticle.ObjectSchemaVerticle;
import com.gentics.mesh.core.verticle.ProjectVerticle;
import com.gentics.mesh.core.verticle.RoleVerticle;
import com.gentics.mesh.core.verticle.TagVerticle;
import com.gentics.mesh.core.verticle.UserVerticle;
import com.gentics.mesh.core.verticle.WebRootVerticle;
import com.gentics.mesh.etc.LanguageEntry;
import com.gentics.mesh.etc.LanguageSet;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.MeshVerticleConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshConfiguration;

@Component
public class BootstrapInitializer {

	private static Logger log = LoggerFactory.getLogger(BootstrapInitializer.class);

	private Map<String, Class<? extends AbstractVerticle>> mandatoryVerticles = new HashMap<>();

	private MeshConfiguration configuration;

	@Autowired
	private MeshRootService rootService;

	@Autowired
	private UserService userService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private SchemaService schemaService;

	@Autowired
	private MeshSpringConfiguration springConfiguration;

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
	public void init(MeshConfiguration configuration, MeshCustomLoader<Vertx> verticleLoader) throws Exception {
		this.configuration = configuration;
		if (configuration.isClusterMode()) {
			joinCluster();
		}

		//		try (Transaction tx = graphDb.beginTx()) {
		initMandatoryData();
		//			tx.success();
		//		}
		loadConfiguredVerticles();
		if (verticleLoader != null) {
			verticleLoader.apply(springConfiguration.vertx());
		}
		//		try (Transaction tx = graphDb.beginTx()) {
		initProjects();
		//			tx.success();
		//		}

	}

	/**
	 * The projects share various subrouters. This method will add the subrouters for all registered projects.
	 * 
	 * @throws InvalidNameException
	 */
	private void initProjects() throws InvalidNameException {
		for (Project project : projectService.findAll()) {
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
		MeshRoot rootNode = null;
		try {
			rootNode = rootService.findRoot();
		} catch (NoSuchElementException e) {
			rootNode = rootService.create();
			log.info("Stored mesh root {" + rootNode.getUuid() + "}");
		}

		LanguageRoot languageRoot;
		try {
			languageRoot = languageService.findRoot();
		} catch (NoSuchElementException e) {

			languageRoot = languageService.createRoot();
			log.info("Stored language root {" + languageRoot.getUuid() + "}");
		}

		GroupRoot groupRoot;
		try {
			groupRoot = groupService.findRoot();
		} catch (NoSuchElementException e) {
			groupRoot = groupService.createRoot();
			log.info("Stored group root {" + groupRoot.getUuid() + "}");
		}

		UserRoot userRoot;
		try {
			userRoot = userService.findRoot();
		} catch (NoSuchElementException e) {
			userRoot = userService.createRoot();
			log.info("Stored user root {" + userRoot.getUuid() + "}");
		}

		RoleRoot roleRoot;
		try {
			roleRoot = roleService.findRoot();
		} catch (NoSuchElementException e) {
			roleRoot = roleService.createRoot();
			log.info("Stored role root {" + roleRoot.getUuid() + "}");
		}

		ProjectRoot projectRoot;
		try {
			projectRoot = projectService.findRoot();
		} catch (NoSuchElementException e) {
			projectRoot = projectService.createRoot();
			log.info("Stored project root {" + projectRoot.getUuid() + "}");
		}

		// Save the default object schemas

		// Content
		Schema contentSchema = schemaService.findByName("content");
		if (contentSchema == null) {
			contentSchema = schemaService.create("content");
			contentSchema.setNestingAllowed(false);
			contentSchema.setDescription("Default schema for contents");
			contentSchema.setDisplayName("Content");

			BasicPropertyType nameProp = schemaService.create(Schema.NAME_KEYWORD, PropertyType.I18N_STRING);
			nameProp.setDisplayName("Name");
			nameProp.setDescription("The name of the content.");
			contentSchema.addPropertyTypeSchema(nameProp);

			BasicPropertyType displayNameProp = schemaService.create(Schema.DISPLAY_NAME_KEYWORD, PropertyType.I18N_STRING);
			displayNameProp.setDisplayName("Display Name");
			displayNameProp.setDescription("The display name property of the content.");
			contentSchema.addPropertyTypeSchema(displayNameProp);

			BasicPropertyType contentProp = schemaService.create(Schema.CONTENT_KEYWORD, PropertyType.I18N_STRING);
			contentProp.setDisplayName("Content");
			contentProp.setDescription("The main content html of the content.");
			contentSchema.addPropertyTypeSchema(contentProp);
			log.info("Stored content schema {" + contentSchema.getUuid() + "}");
		}

		// Folder
		Schema folderSchema = schemaService.findByName("folder");
		if (folderSchema == null) {
			folderSchema = schemaService.create("folder");
			folderSchema.setNestingAllowed(true);
			folderSchema.setDescription("Default schema for folders");
			folderSchema.setDisplayName("Folder");

			BasicPropertyType nameProp = schemaService.create(Schema.NAME_KEYWORD, PropertyType.I18N_STRING);
			nameProp.setDisplayName("Name");
			nameProp.setDescription("The name of the folder.");
			folderSchema.addPropertyTypeSchema(nameProp);
			log.info("Stored folder schema  {" + folderSchema.getUuid() + "}");
		}

		// Binary content for images and other downloads
		Schema binarySchema = schemaService.findByName("binary-content");
		if (binarySchema == null) {
			binarySchema = schemaService.create("binary-content");
			binarySchema.setDescription("Default schema for binary contents");
			binarySchema.setDisplayName("Binary Content");

			BasicPropertyType nameProp = schemaService.create(Schema.NAME_KEYWORD, PropertyType.I18N_STRING);
			nameProp.setDisplayName("Name");
			nameProp.setDescription("The name of the content.");
			binarySchema.addPropertyTypeSchema(nameProp);

			BasicPropertyType displayNameProp = schemaService.create(Schema.DISPLAY_NAME_KEYWORD, PropertyType.I18N_STRING);
			displayNameProp.setDisplayName("Display Name");
			displayNameProp.setDescription("The display name property of the content.");
			binarySchema.addPropertyTypeSchema(displayNameProp);

		}

		// Tag schema
		Schema tagSchema = schemaService.findByName("tag");
		if (tagSchema == null) {
			tagSchema = schemaService.create("tag");
			tagSchema.setDisplayName("Tag");
			tagSchema.setDescription("Default schema for tags");
			tagSchema.addPropertyTypeSchema(schemaService.create(Schema.NAME_KEYWORD, PropertyType.I18N_STRING));
			tagSchema.addPropertyTypeSchema(schemaService.create(Schema.DISPLAY_NAME_KEYWORD, PropertyType.I18N_STRING));
			tagSchema.addPropertyTypeSchema(schemaService.create(Schema.CONTENT_KEYWORD, PropertyType.I18N_STRING));
			log.info("Stored tag schema {" + tagSchema.getUuid() + "}");
		}

		SchemaRoot schemaRoot;
		try {
			schemaRoot = schemaService.findRoot();
		} catch (NoSuchElementException e) {
			schemaRoot = schemaService.createRoot();
			schemaRoot.addSchema(tagSchema);
			schemaRoot.addSchema(contentSchema);
			schemaRoot.addSchema(binarySchema);
			log.info("Stored schema root node");
		}

		// Verify that the root node is existing
		rootNode.setProjectRoot(projectRoot);
		rootNode.setGroupRoot(groupRoot);
		rootNode.setRoleRoot(roleRoot);
		rootNode.setLanguageRoot(languageRoot);
		rootNode.setSchemaRoot(schemaRoot);
		rootNode.setUserRoot(userRoot);
		log.info("Stored mesh root node");

		initLanguages(languageRoot);

		// Verify that an admin user exists
		User adminUser;
		try {
			adminUser = userService.findByUsername("admin");
		} catch (NoSuchElementException e) {
			adminUser = userService.create("admin");
			System.out.println("Enter admin password:");
			// Scanner scanIn = new Scanner(System.in);
			// String pw = scanIn.nextLine();
			// TODO remove later on
			String pw = "finger";
			// scanIn.close();
			adminUser.setPasswordHash(springConfiguration.passwordEncoder().encode(pw));
			log.info("Stored admin user");
		}
		rootNode.getUserRoot().addUser(adminUser);

		Group adminGroup;
		try {
			adminGroup = groupService.findByName("admin");
		} catch (NoSuchElementException e) {
			adminGroup = groupService.create("admin");
			adminGroup.addUser(adminUser);
			log.info("Stored admin group");
		}
		rootNode.getGroupRoot().addGroup(adminGroup);

		Role adminRole;
		try {
			adminRole = roleService.findByName("admin");
		} catch (NoSuchElementException e) {
			adminRole = roleService.create("admin");
			adminGroup.addRole(adminRole);
		}

	}

	protected void initLanguages(LanguageRoot rootNode) throws JsonParseException, JsonMappingException, IOException {

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
			Language language;
			try {
				language = languageService.findByName(languageName);
			} catch (NoSuchElementException e) {
				language = languageService.create(languageName, languageTag);
				language.setNativeName(languageNativeName);
				rootNode.addLanguage(language);
				log.debug("Added language {" + languageTag + " / " + languageName + "}");
			}
		}
		long diff = System.currentTimeMillis() - start;
		log.info("Handling languages took: " + diff + " [ms]");
	}
}
