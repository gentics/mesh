package com.gentics.mesh.cli;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
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
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.SearchQueueRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.verticle.AdminVerticle;
import com.gentics.mesh.core.verticle.AuthenticationVerticle;
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
import com.gentics.mesh.etc.config.MeshOptions;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

@Component
public class BootstrapInitializer {

	private static Logger log = LoggerFactory.getLogger(BootstrapInitializer.class);

	private Map<String, Class<? extends AbstractVerticle>> mandatoryVerticles = new HashMap<>();

	private MeshOptions configuration;

	@Autowired
	private ServerSchemaStorage schemaStorage;

	private static BootstrapInitializer instance;
	private static MeshRoot meshRoot;

	@PostConstruct
	public void setup() {
		instance = this;
		clearReferences();
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
		addMandatoryVerticle(AuthenticationVerticle.class);
		addMandatoryVerticle(AdminVerticle.class);

	}

	private void addMandatoryVerticle(Class<? extends AbstractVerticle> clazz) {
		mandatoryVerticles.put(clazz.getSimpleName(), clazz);
	}

	public MeshRoot meshRoot() {
		// Check reference graph and finally create the node when it can't be found.
		if (meshRoot == null) {
			meshRoot = fg.v().has(MeshRootImpl.class).nextOrDefault(MeshRootImpl.class, null);
			if (meshRoot == null) {
				meshRoot = fg.addFramedVertex(MeshRootImpl.class);
				log.info("Stored mesh root {" + meshRoot.getUuid() + "}");
			}
		}
		return meshRoot;
	}

	public SchemaContainerRoot findSchemaContainerRoot() {
		return meshRoot().getSchemaContainerRoot();
	}

	public SchemaContainerRoot schemaContainerRoot() {
		return meshRoot().getSchemaContainerRoot();
	}

	public MicroschemaContainerRoot microschemaContainerRoot() {
		return meshRoot().getMicroschemaContainerRoot();
	}

	public RoleRoot roleRoot() {
		return meshRoot().getRoleRoot();
	}

	public TagRoot tagRoot() {
		return meshRoot().getTagRoot();
	}

	public TagFamilyRoot tagFamilyRoot() {
		return meshRoot().getTagFamilyRoot();
	}

	public NodeRoot nodeRoot() {
		return meshRoot().getNodeRoot();
	}

	public UserRoot userRoot() {
		return meshRoot().getUserRoot();
	}

	public GroupRoot groupRoot() {
		return meshRoot().getGroupRoot();
	}

	public LanguageRoot languageRoot() {
		return meshRoot().getLanguageRoot();
	}

	public ProjectRoot projectRoot() {
		return meshRoot().getProjectRoot();
	}

	public static void clearReferences() {
		if (meshRoot != null) {
			meshRoot.clearReferences();
			meshRoot = null;
		}
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
				deployAndWait(Mesh.vertx(), defaultConfig, clazz);
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
				deployAndWait(Mesh.vertx(), mergedVerticleConfig, verticleName);
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
	public void init(MeshOptions configuration, MeshCustomLoader<Vertx> verticleLoader) throws Exception {
		this.configuration = configuration;
		if (configuration.isClusterMode()) {
			joinCluster();
		}

		initMandatoryData();
		loadConfiguredVerticles();
		if (verticleLoader != null) {
			verticleLoader.apply(Mesh.vertx());
		}
		initProjects();
		Mesh.vertx().eventBus().send("mesh-startup-complete", true);

	}

	/**
	 * The projects share various subrouters. This method will add the subrouters for all registered projects.
	 * 
	 * @throws InvalidNameException
	 */
	private void initProjects() throws InvalidNameException {
		for (Project project : meshRoot().getProjectRoot().findAll()) {
			routerStorage.addProjectRouter(project.getName());
			log.info("Initalized project {" + project.getName() + "}");
		}
	}

	/**
	 * Use the hazelcast cluster manager to join the cluster of mesh instances.
	 */
	private void joinCluster() {
		HazelcastClusterManager manager = new HazelcastClusterManager();
		manager.setVertx(Mesh.vertx());
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
		MeshRootImpl.setInstance(meshRoot);

		NodeRoot nodeRoot = meshRoot.getNodeRoot();
		TagRoot tagRoot = meshRoot.getTagRoot();
		TagFamilyRoot tagFamilyRoot = meshRoot.getTagFamilyRoot();
		LanguageRoot languageRoot = meshRoot.getLanguageRoot();
		GroupRoot groupRoot = meshRoot.getGroupRoot();
		UserRoot userRoot = meshRoot.getUserRoot();
		RoleRoot roleRoot = meshRoot.getRoleRoot();
		ProjectRoot projectRoot = meshRoot.getProjectRoot();
		SchemaContainerRoot schemaContainerRoot = meshRoot.getSchemaContainerRoot();

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
			nameFieldSchema.setRequired(true);
			schema.addField(nameFieldSchema);

			StringFieldSchema filenameFieldSchema = new StringFieldSchemaImpl();
			filenameFieldSchema.setName("filename");
			filenameFieldSchema.setLabel("Filename");
			schema.addField(filenameFieldSchema);

			StringFieldSchema titleFieldSchema = new StringFieldSchemaImpl();
			titleFieldSchema.setName("title");
			titleFieldSchema.setLabel("Title");
			schema.addField(titleFieldSchema);

			HtmlFieldSchema contentFieldSchema = new HtmlFieldSchemaImpl();
			contentFieldSchema.setName("content");
			contentFieldSchema.setLabel("Content");
			schema.addField(contentFieldSchema);

			schema.setBinary(false);
			schema.setFolder(false);
			contentSchemaContainer = schemaContainerRoot.create(schema);

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
			schema.addField(nameFieldSchema);

			schema.setBinary(false);
			schema.setFolder(true);
			folderSchemaContainer = schemaContainerRoot.create(schema);

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
			schema.addField(nameFieldSchema);

			StringFieldSchema filenameFieldSchema = new StringFieldSchemaImpl();
			nameFieldSchema.setName("filename");
			nameFieldSchema.setLabel("Filename");
			schema.addField(filenameFieldSchema);

			schema.setBinary(true);
			schema.setFolder(false);
			binarySchemaContainer = schemaContainerRoot.create(schema);
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

		initPermissions(adminRole);

		schemaStorage.init();

	}

	private void initPermissions(Role role) {
		for (Vertex vertex : fg.getVertices()) {
			WrappedVertex wrappedVertex = (WrappedVertex) vertex;

			// TODO typecheck? and verify how orient will behave
			if (role.getUuid().equalsIgnoreCase(vertex.getProperty("uuid"))) {
				log.info("Skipping own role");
				continue;
			}
			MeshVertex meshVertex = fg.frameElement(wrappedVertex.getBaseElement(), MeshVertexImpl.class);
			role.addPermissions(meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM);
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
			Language language = meshRoot().getLanguageRoot().findByName(languageName);
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
