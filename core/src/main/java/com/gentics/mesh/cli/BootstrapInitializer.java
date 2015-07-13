package com.gentics.mesh.cli;

import static com.gentics.mesh.util.DeploymentUtil.deployAndWait;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.naming.InvalidNameException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.schema.HTMLFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.HTMLFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.verticle.AdminVerticle;
import com.gentics.mesh.core.verticle.GroupVerticle;
import com.gentics.mesh.core.verticle.ProjectVerticle;
import com.gentics.mesh.core.verticle.RoleVerticle;
import com.gentics.mesh.core.verticle.SchemaVerticle;
import com.gentics.mesh.core.verticle.UserVerticle;
import com.gentics.mesh.core.verticle.WebRootVerticle;
import com.gentics.mesh.core.verticle.project.ProjectNodeVerticle;
import com.gentics.mesh.core.verticle.project.ProjectTagFamilyVerticle;
import com.gentics.mesh.core.verticle.project.ProjectTagVerticle;
import com.gentics.mesh.etc.LanguageEntry;
import com.gentics.mesh.etc.LanguageSet;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.MeshVerticleConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshConfiguration;
import com.syncleus.ferma.FramedGraph;

@Component
public class BootstrapInitializer {

	private static Logger log = LoggerFactory.getLogger(BootstrapInitializer.class);

	private Map<String, Class<? extends AbstractVerticle>> mandatoryVerticles = new HashMap<>();

	private MeshConfiguration configuration;

	@Autowired
	private ServerSchemaStorage schemaStorage;

	private static BootstrapInitializer instance;

	@PostConstruct
	public void setup() {
		instance = this;
		clearReferenceCache();
	}

	public static BootstrapInitializer getBoot() {
		return instance;
	}

	@Autowired
	private FramedGraph fg;

	@Autowired
	private MeshSpringConfiguration springConfiguration;

	@Autowired
	private RouterStorage routerStorage;

	private static GroupRoot groupRoot;
	private static NodeRoot nodeRoot;
	private static TagRoot tagRoot;
	private static TagFamilyRoot tagFamilyRoot;
	private static LanguageRoot languageRoot;
	private static RoleRoot roleRoot;
	private static UserRoot userRoot;
	private static SchemaContainerRoot schemaContainerRoot;
	private static MicroschemaContainerRoot microschemaContainerRoot;
	private static ProjectRoot projectRoot;
	private static MeshRoot meshRoot;

	public BootstrapInitializer() {
		addMandatoryVerticle(UserVerticle.class);
		addMandatoryVerticle(GroupVerticle.class);
		addMandatoryVerticle(RoleVerticle.class);

		addMandatoryVerticle(ProjectTagVerticle.class);
		addMandatoryVerticle(ProjectNodeVerticle.class);
		addMandatoryVerticle(ProjectTagFamilyVerticle.class);
		addMandatoryVerticle(WebRootVerticle.class);

		addMandatoryVerticle(ProjectVerticle.class);
		addMandatoryVerticle(SchemaVerticle.class);
		// addMandatoryVerticle(SearchVerticle.class);
		// addMandatoryVerticle(AuthenticationVerticle.class);
		addMandatoryVerticle(AdminVerticle.class);

	}

	private void addMandatoryVerticle(Class<? extends AbstractVerticle> clazz) {
		mandatoryVerticles.put(clazz.getSimpleName(), clazz);
	}

	public MeshRoot createMeshRoot() {
		MeshRootImpl root = fg.addFramedVertex(MeshRootImpl.class);
		return root;
	}

	public MeshRoot findMeshRoot() {
		return fg.v().has(MeshRootImpl.class).nextOrDefault(MeshRootImpl.class, null);
	}

	public MeshRoot meshRoot() {
		if (meshRoot == null) {
			meshRoot = findMeshRoot();
		}
		return meshRoot;
	}

	public SchemaContainerRoot findSchemaContainerRoot() {
		return meshRoot().getSchemaContainerRoot();
	}

	public SchemaContainerRoot schemaContainerRoot() {
		if (schemaContainerRoot == null) {
			schemaContainerRoot = findSchemaContainerRoot();
		}
		return schemaContainerRoot;
	}

	public MicroschemaContainerRoot findMicroschemaContainerRoot() {
		return meshRoot().getMicroschemaContainerRoot();
	}

	public MicroschemaContainerRoot microschemaContainerRoot() {
		if (microschemaContainerRoot == null) {
			microschemaContainerRoot = findMicroschemaContainerRoot();
		}
		return microschemaContainerRoot;
	}

	public RoleRoot findRoleRoot() {
		return meshRoot().getRoleRoot();
	}

	public RoleRoot roleRoot() {
		if (roleRoot == null) {
			roleRoot = findRoleRoot();
		}
		return roleRoot;
	}

	public TagRoot findTagRoot() {
		return meshRoot().getTagRoot();
	}

	public TagRoot tagRoot() {
		if (tagRoot == null) {
			tagRoot = findTagRoot();
		}
		return tagRoot;
	}

	public TagFamilyRoot findTagFamilyRoot() {
		return meshRoot().getTagFamilyRoot();
	}

	public TagFamilyRoot tagFamilyRoot() {
		if (tagFamilyRoot == null) {
			tagFamilyRoot = findTagFamilyRoot();
		}
		return tagFamilyRoot;

	}

	public NodeRoot findNodeRoot() {
		return meshRoot().getNodeRoot();
	}

	public NodeRoot nodeRoot() {
		if (nodeRoot == null) {
			nodeRoot = findNodeRoot();
		}
		return nodeRoot;
	}

	public UserRoot findUserRoot() {
		return meshRoot().getUserRoot();
	}

	public UserRoot userRoot() {
		if (userRoot == null) {
			return findUserRoot();
		}
		return userRoot;
	}

	public GroupRoot findGroupRoot() {
		return meshRoot().getGroupRoot();
	}

	public GroupRoot groupRoot() {
		if (groupRoot == null) {
			groupRoot = findGroupRoot();
		}
		return groupRoot;
	}

	public LanguageRoot findLanguageRoot() {
		return meshRoot().getLanguageRoot();
	}

	public LanguageRoot languageRoot() {
		if (languageRoot == null) {
			languageRoot = findLanguageRoot();
		}
		return languageRoot;
	}

	public ProjectRoot findProjectRoot() {
		return meshRoot().getProjectRoot();
	}

	public ProjectRoot projectRoot() {
		if (projectRoot == null) {
			projectRoot = findProjectRoot();
		}
		return projectRoot;
	}

	public static void clearReferenceCache() {
		projectRoot = null;
		tagRoot = null;
		roleRoot = null;
		meshRoot = null;
		groupRoot = null;
		userRoot = null;
		nodeRoot = null;
		schemaContainerRoot = null;
		languageRoot = null;
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

		initMandatoryData();
		loadConfiguredVerticles();
		if (verticleLoader != null) {
			verticleLoader.apply(springConfiguration.vertx());
		}
		initProjects();
		springConfiguration.vertx().eventBus().send("mesh-startup-complete", true);

	}

	/**
	 * The projects share various subrouters. This method will add the subrouters for all registered projects.
	 * 
	 * @throws InvalidNameException
	 */
	private void initProjects() throws InvalidNameException {
		for (Project project : projectRoot().findAll()) {
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
		MeshRoot meshRoot = meshRoot();
		if (meshRoot == null) {
			meshRoot = createMeshRoot();
			log.info("Stored mesh root {" + meshRoot.getUuid() + "}");
		}
		MeshRootImpl.setInstance(meshRoot);

		NodeRoot nodeRoot = meshRoot.getNodeRoot();
		if (nodeRoot == null) {
			nodeRoot = meshRoot.createNodeRoot();
			log.info("Stored node root {" + nodeRoot.getUuid() + "}");
		}

		TagRoot tagRoot = meshRoot.getTagRoot();
		if (tagRoot == null) {
			tagRoot = meshRoot.createTagRoot();
			log.info("Stored tag root {" + tagRoot.getUuid() + "}");
		}

		TagFamilyRoot tagFamilyRoot = meshRoot.getTagFamilyRoot();
		if (tagFamilyRoot == null) {
			tagFamilyRoot = meshRoot.createTagFamilyRoot();
			log.info("Stored tag family root {" + tagFamilyRoot.getUuid() + "}");
		}

		LanguageRoot languageRoot = meshRoot.getLanguageRoot();
		if (languageRoot == null) {
			languageRoot = meshRoot.createLanguageRoot();
			log.info("Stored language root {" + languageRoot.getUuid() + "}");
		}

		GroupRoot groupRoot = meshRoot.getGroupRoot();
		if (groupRoot == null) {
			groupRoot = meshRoot.createGroupRoot();
			log.info("Stored group root {" + groupRoot.getUuid() + "}");
		}

		UserRoot userRoot = meshRoot.getUserRoot();
		if (userRoot == null) {
			userRoot = meshRoot.createUserRoot();
			log.info("Stored user root {" + userRoot.getUuid() + "}");
		}

		RoleRoot roleRoot = meshRoot.getRoleRoot();
		if (roleRoot == null) {
			roleRoot = meshRoot.createRoleRoot();
			log.info("Stored role root {" + roleRoot.getUuid() + "}");
		}

		ProjectRoot projectRoot = meshRoot.getProjectRoot();
		if (projectRoot == null) {
			projectRoot = meshRoot.createProjectRoot();
			log.info("Stored project root {" + projectRoot.getUuid() + "}");
		}

		// Save the default object schemas
		SchemaContainerRoot schemaContainerRoot = meshRoot.getSchemaContainerRoot();
		if (schemaContainerRoot == null) {
			schemaContainerRoot = meshRoot.createRoot();
			log.info("Stored schema root node");
		}

		// Content
		SchemaContainer contentSchemaContainer = schemaContainerRoot.findByName("content");
		if (contentSchemaContainer == null) {
			Schema schema = new SchemaImpl();
			schema.setName("content");
			schema.setDisplayField("title");
			schema.setMeshVersion(Mesh.getVersion());
			schema.setSchemaVersion("1.0.0");

			StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
			nameFieldSchema.setName("name");
			nameFieldSchema.setLabel("Name");
			nameFieldSchema.setString("Enter the name here");
			schema.addField("name", nameFieldSchema);

			StringFieldSchema filenameFieldSchema = new StringFieldSchemaImpl();
			filenameFieldSchema.setName("filename");
			filenameFieldSchema.setLabel("Filename");
			filenameFieldSchema.setString("Enter the filename here");
			schema.addField("filename", filenameFieldSchema);

			StringFieldSchema titleFieldSchema = new StringFieldSchemaImpl();
			titleFieldSchema.setName("title");
			titleFieldSchema.setLabel("Title");
			titleFieldSchema.setString("Enter the title here");
			schema.addField("title", titleFieldSchema);

			HTMLFieldSchema contentFieldSchema = new HTMLFieldSchemaImpl();
			titleFieldSchema.setName("content");
			titleFieldSchema.setLabel("Content");
			titleFieldSchema.setString("Enter your text here");
			schema.addField("content", contentFieldSchema);

			schema.setBinary(false);
			schema.setContainer(false);
			contentSchemaContainer = schemaContainerRoot.create("content");
			contentSchemaContainer.setSchema(schema);

		}

		// Folder
		SchemaContainer folderSchemaContainer = schemaContainerRoot.findByName("folder");
		if (folderSchemaContainer == null) {
			Schema schema = new SchemaImpl();
			schema.setName("folder");
			schema.setDisplayField("name");
			schema.setMeshVersion(Mesh.getVersion());
			schema.setSchemaVersion("1.0.0");

			StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
			nameFieldSchema.setName("name");
			nameFieldSchema.setLabel("Name");
			schema.addField("name", nameFieldSchema);

			schema.setBinary(false);
			schema.setContainer(true);
			folderSchemaContainer = schemaContainerRoot.create("folder");
			folderSchemaContainer.setSchema(schema);

		}

		// Binary content for images and other downloads
		SchemaContainer binarySchemaContainer = schemaContainerRoot.findByName("binary-content");
		if (binarySchemaContainer == null) {

			Schema schema = new SchemaImpl();
			schema.setName("binary-content");
			schema.setDisplayField("name");
			schema.setMeshVersion(Mesh.getVersion());
			schema.setSchemaVersion("1.0.0");

			StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
			nameFieldSchema.setName("name");
			nameFieldSchema.setLabel("Name");
			schema.addField("name", nameFieldSchema);

			StringFieldSchema filenameFieldSchema = new StringFieldSchemaImpl();
			nameFieldSchema.setName("filename");
			nameFieldSchema.setLabel("Filename");
			schema.addField("filename", filenameFieldSchema);

			schema.setBinary(true);
			schema.setContainer(false);
			binarySchemaContainer = schemaContainerRoot.create("binary-content");
			binarySchemaContainer.setSchema(schema);
		}

		log.info("Stored mesh root node");

		initLanguages(languageRoot);

		// Verify that an admin user exists
		User adminUser = userRoot.findByUsername("admin");
		if (adminUser == null) {
			adminUser = userRoot.create("admin");

			adminUser.setCreator(adminUser);
			adminUser.setCreationTimestamp(System.currentTimeMillis());
			adminUser.setEditor(adminUser);
			adminUser.setLastEditedTimestamp(System.currentTimeMillis());

			System.out.println("Enter admin password:");
			// Scanner scanIn = new Scanner(System.in);
			// String pw = scanIn.nextLine();
			// TODO remove later on
			String pw = "finger";
			// scanIn.close();
			adminUser.setPasswordHash(springConfiguration.passwordEncoder().encode(pw));
			log.info("Stored admin user");
		}

		Group adminGroup = groupRoot.findByName("admin");
		if (adminGroup == null) {
			adminGroup = groupRoot.create("admin");
			adminGroup.addUser(adminUser);
			log.info("Stored admin group");
		}

		Role adminRole = roleRoot.findByName("admin");
		if (adminRole == null) {
			adminRole = roleRoot.create("admin");
			adminGroup.addRole(adminRole);
		}

		schemaStorage.init();

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
			Language language = languageRoot().findByName(languageName);
			if (language == null) {
				language = rootNode.create(languageName, languageTag);
				language.setNativeName(languageNativeName);
				log.debug("Added language {" + languageTag + " / " + languageName + "}");
			}
		}
		long diff = System.currentTimeMillis() - start;
		log.info("Handling languages took: " + diff + " [ms]");
	}

}
