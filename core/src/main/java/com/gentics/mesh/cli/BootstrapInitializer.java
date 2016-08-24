package com.gentics.mesh.cli;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.DeploymentUtil.deployAndWait;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.naming.InvalidNameException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.changelog.ChangelogSystem;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
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
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.verticle.admin.AdminVerticle;
import com.gentics.mesh.core.verticle.admin.RestInfoVerticle;
import com.gentics.mesh.core.verticle.auth.AuthenticationVerticle;
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;
import com.gentics.mesh.core.verticle.navroot.NavRootVerticle;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.project.ProjectInfoVerticle;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.core.verticle.release.ReleaseVerticle;
import com.gentics.mesh.core.verticle.role.RoleVerticle;
import com.gentics.mesh.core.verticle.schema.ProjectSchemaVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.core.verticle.utility.UtilityVerticle;
import com.gentics.mesh.core.verticle.webroot.WebRootVerticle;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.LanguageEntry;
import com.gentics.mesh.etc.LanguageSet;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshVerticleConfiguration;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.ProjectSearchVerticle;
import com.gentics.mesh.search.SearchVerticle;
import com.gentics.mesh.search.index.IndexHandler;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * The bootstrap initializer takes care of creating all mandatory graph elements for mesh. This includes the creation of MeshRoot, ProjectRoot, NodeRoot,
 * GroupRoot, UserRoot and various element such as the Admin User, Admin Group, Admin Role.
 */
public class BootstrapInitializer {

	private static Logger log = LoggerFactory.getLogger(BootstrapInitializer.class);

	private MeshOptions configuration;

	private ServerSchemaStorage schemaStorage;

	private Database db;

	private IndexHandlerRegistry searchHandlerRegistry;

	private static BootstrapInitializer instance;

	private Map<String, Class<? extends AbstractVerticle>> mandatoryVerticles = new HashMap<>();

	private Map<String, Class<? extends AbstractVerticle>> mandatoryWorkerVerticles = new HashMap<>();

	private MeshSpringConfiguration springConfiguration;

	private RouterStorage routerStorage;

	private static MeshRoot meshRoot;

	public static boolean isInitialSetup = true;

	@Inject
	public BootstrapInitializer(Database db, IndexHandlerRegistry searchHandlerRegistry,
			MeshSpringConfiguration springConfiguration, RouterStorage routerStorage) {
		this.db = db;
		this.searchHandlerRegistry = searchHandlerRegistry;
		this.springConfiguration = springConfiguration;
		this.routerStorage = routerStorage;

		// Add API Info Verticle
		addMandatoryVerticle(RestInfoVerticle.class);
		addMandatoryVerticle(ProjectInfoVerticle.class);

		// User Group Role verticles
		addMandatoryVerticle(UserVerticle.class);
		addMandatoryVerticle(GroupVerticle.class);
		addMandatoryVerticle(RoleVerticle.class);

		// Project specific verticles
		addMandatoryVerticle(NodeVerticle.class);
		addMandatoryVerticle(TagFamilyVerticle.class);
		addMandatoryVerticle(ProjectSchemaVerticle.class);
		addMandatoryVerticle(ReleaseVerticle.class);

		// Global verticles
		addMandatoryVerticle(WebRootVerticle.class);
		addMandatoryVerticle(NavRootVerticle.class);
		addMandatoryVerticle(ProjectVerticle.class);
		addMandatoryVerticle(SchemaVerticle.class);
		addMandatoryVerticle(MicroschemaVerticle.class);
		addMandatoryVerticle(SearchVerticle.class);
		addMandatoryVerticle(ProjectSearchVerticle.class);
		addMandatoryVerticle(AuthenticationVerticle.class);
		addMandatoryVerticle(AdminVerticle.class);
		addMandatoryVerticle(EventbusVerticle.class);
		addMandatoryVerticle(UtilityVerticle.class);

		// Worker verticles
		addMandatoryWorkerVerticle(NodeMigrationVerticle.class);
	}

	/**
	 * Add the given verticle class to the list of mandatory verticles
	 * 
	 * @param clazz
	 */
	private void addMandatoryVerticle(Class<? extends AbstractVerticle> clazz) {
		mandatoryVerticles.put(clazz.getSimpleName(), clazz);
	}

	private Map<String, Class<? extends AbstractVerticle>> getMandatoryVerticleClasses() {
		return mandatoryVerticles;
	}

	/**
	 * Add the given verticle class to the list of mandatory worker verticles
	 *
	 * @param clazz
	 */
	private void addMandatoryWorkerVerticle(Class<? extends AbstractVerticle> clazz) {
		mandatoryWorkerVerticles.put(clazz.getSimpleName(), clazz);
	}

	/**
	 * Get the map of mandatory worker verticle classes
	 * 
	 * @return
	 */
	private Map<String, Class<? extends AbstractVerticle>> getMandatoryWorkerVerticleClasses() {
		return mandatoryWorkerVerticles;
	}

	/**
	 * The projects share various subrouters. This method will add the subrouters for all registered projects.
	 * 
	 * @throws InvalidNameException
	 */
	private void initProjects() throws InvalidNameException {
		for (Project project : meshRoot().getProjectRoot().findAll()) {
			routerStorage.addProjectRouter(project.getName());
			if (log.isInfoEnabled()) {
				log.info("Initalized project {" + project.getName() + "}");
			}
		}
	}

	/**
	 * Use the hazelcast cluster manager to join the cluster of mesh instances.
	 */
	private void joinCluster() {
		log.info("Joining cluster...");
		HazelcastClusterManager manager = new HazelcastClusterManager();
		manager.setVertx(Mesh.vertx());
		manager.join(rh -> {
			if (!rh.succeeded()) {
				log.error("Error while joining mesh cluster.", rh.cause());
			}
		});
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

		// Only execute the installation if there are any elements in the graph
		boolean isEmptyInstallation = isEmptyInstallation();
		if (!isEmptyInstallation) {
			invokeChangelog();
		}

		initMandatoryData();

		// Mark all changelog entries as applied for new installations
		if (isEmptyInstallation) {
			markChangelogApplied();
		}

		//initPermissions();
		initSearchIndex();
		try {
			invokeSearchQueueProcessing();
		} catch (Exception e) {
			log.error("Could not handle existing search queue entries", e);
		}

		loadConfiguredVerticles();
		if (verticleLoader != null) {
			verticleLoader.apply(Mesh.vertx());
		}
		db.asyncNoTx(() -> {
			initProjects();
			return null;
		}).toBlocking().value();
		log.info("Sending startup completed event to {" + Mesh.STARTUP_EVENT_ADDRESS + "}");
		Mesh.vertx().eventBus().publish(Mesh.STARTUP_EVENT_ADDRESS, true);

	}

	/**
	 * Check whether there are any vertices in the graph.
	 * 
	 * @return
	 */
	private boolean isEmptyInstallation() {
		try (NoTx noTx = db.noTx()) {
			return noTx.getGraph().v().count() == 0;
		}
	}

	/**
	 * Process remaining search queue batches.
	 * 
	 * @throws InterruptedException
	 */
	private void invokeSearchQueueProcessing() throws InterruptedException {
		db.tx(() -> {
			log.info("Starting search queue processing of remaining entries...");
			long processed = meshRoot().getSearchQueue().processAll();
			log.info("Processed {" + processed + "} elements.");
			return null;
		});
	}

	/**
	 * Invoke the changelog system to execute database changes.
	 */
	public void invokeChangelog() {
		log.info("Invoking database changelog check...");
		ChangelogSystem cls = new ChangelogSystem(db);
		if (!cls.applyChanges()) {
			throw new RuntimeException("The changelog could not be applied successfully. See log above.");
		}
	}

	/***
	 * Marking all changes as applied since this is an initial mesh setup
	 */
	private void markChangelogApplied() {
		log.info("This is the initial setup.. marking all found changelog entries as applied");
		ChangelogSystem cls = new ChangelogSystem(db);
		cls.markAllAsApplied();

	}

	/**
	 * Initialize the search queue handlers.
	 */
	public void initSearchIndex() {
		for (IndexHandler handler : searchHandlerRegistry.getHandlers()) {
			handler.init().await();
		}
	}

	/**
	 * Load verticles that are configured within the mesh configuration.
	 * 
	 * @throws InterruptedException
	 */
	private void loadConfiguredVerticles() throws InterruptedException {
		JsonObject defaultConfig = new JsonObject();
		defaultConfig.put("port", configuration.getHttpServerOptions().getPort());

		for (Class<? extends AbstractVerticle> clazz : getMandatoryVerticleClasses().values()) {
			try {
				if (log.isInfoEnabled()) {
					log.info("Loading mandatory verticle {" + clazz.getName() + "}.");
				}
				// TODO handle custom config? i assume we will not allow this
				deployAndWait(Mesh.vertx(), defaultConfig, clazz, false);
			} catch (InterruptedException e) {
				log.error("Could not load mandatory verticle {" + clazz.getSimpleName() + "}.", e);
			}
		}

		for (Class<? extends AbstractVerticle> clazz : getMandatoryWorkerVerticleClasses().values()) {
			try {
				if (log.isInfoEnabled()) {
					log.info("Loading mandatory verticle {" + clazz.getName() + "}.");
				}
				// TODO handle custom config? i assume we will not allow this
				deployAndWait(Mesh.vertx(), defaultConfig, clazz, true);
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
			mergedVerticleConfig.put("port", configuration.getHttpServerOptions().getPort());
			try {
				if (log.isInfoEnabled()) {
					log.info("Loading configured verticle {" + verticleName + "}.");
				}
				deployAndWait(Mesh.vertx(), mergedVerticleConfig, verticleName, false);
			} catch (InterruptedException e) {
				log.error("Could not load verticle {" + verticleName + "}.", e);
			}
		}

	}

	@PostConstruct
	public void setup() {
		instance = this;
		clearReferences();
	}

	public static BootstrapInitializer getBoot() {
		return instance;
	}

	/**
	 * Return the mesh root node. This method will also create the node if it could not be found within the graph.
	 * 
	 * @return
	 */
	public MeshRoot meshRoot() {
		if (meshRoot == null) {
			synchronized (BootstrapInitializer.class) {
				// Check reference graph and finally create the node when it can't be found.
				MeshRoot foundMeshRoot = Database.getThreadLocalGraph().v().has(MeshRootImpl.class).nextOrDefault(MeshRootImpl.class, null);
				if (foundMeshRoot == null) {

					meshRoot = Database.getThreadLocalGraph().addFramedVertex(MeshRootImpl.class);
					if (log.isInfoEnabled()) {
						log.info("Created mesh root {" + meshRoot.getUuid() + "}");
					}
				} else {
					isInitialSetup = false;
					meshRoot = foundMeshRoot;
				}
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
		BootstrapInitializer.meshRoot = null;
		MeshRootImpl.clearReferences();
	}

	/**
	 * Setup various mandatory data. This includes mandatory root nodes and the admin user, group.
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws MeshSchemaException
	 */
	public void initMandatoryData() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException {
		Role adminRole;
		MeshRoot meshRoot;

		try (Tx tx = db.tx()) {
			meshRoot = meshRoot();
			MeshRootImpl.setInstance(meshRoot);

			meshRoot.getNodeRoot();
			meshRoot.getTagRoot();
			meshRoot.getTagFamilyRoot();
			meshRoot.getProjectRoot();
			meshRoot.getSearchQueue();
			meshRoot.getLanguageRoot();

			GroupRoot groupRoot = meshRoot.getGroupRoot();
			UserRoot userRoot = meshRoot.getUserRoot();
			RoleRoot roleRoot = meshRoot.getRoleRoot();
			SchemaContainerRoot schemaContainerRoot = meshRoot.getSchemaContainerRoot();

			// Verify that an admin user exists
			User adminUser = userRoot.findByUsername("admin");
			if (adminUser == null) {
				adminUser = userRoot.create("admin", adminUser);

				adminUser.setCreator(adminUser);
				adminUser.setCreationTimestamp(System.currentTimeMillis());
				adminUser.setEditor(adminUser);
				adminUser.setLastEditedTimestamp(System.currentTimeMillis());

				log.info("Enter admin password:");
				// Scanner scanIn = new Scanner(System.in);
				// String pw = scanIn.nextLine();
				// TODO remove later on
				String pw = "admin";
				// scanIn.close();
				adminUser.setPasswordHash(springConfiguration.passwordEncoder().encode(pw));
				log.info("Created admin user {" + adminUser.getUuid() + "}");
			}

			// Content
			SchemaContainer contentSchemaContainer = schemaContainerRoot.findByName("content").toBlocking().value();
			if (contentSchemaContainer == null) {
				Schema schema = new SchemaModel();
				schema.setName("content");
				schema.setDisplayField("title");
				schema.setSegmentField("filename");

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

				schema.setContainer(false);
				contentSchemaContainer = schemaContainerRoot.create(schema, adminUser);
				log.info("Created schema container {" + schema.getName() + "} uuid: {" + contentSchemaContainer.getUuid() + "}");
			}

			// Folder
			SchemaContainer folderSchemaContainer = schemaContainerRoot.findByName("folder").toBlocking().value();
			if (folderSchemaContainer == null) {
				Schema schema = new SchemaModel();
				schema.setName("folder");
				schema.setDisplayField("name");
				schema.setSegmentField("name");

				StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
				nameFieldSchema.setName("name");
				nameFieldSchema.setLabel("Name");
				schema.addField(nameFieldSchema);

				schema.setContainer(true);
				folderSchemaContainer = schemaContainerRoot.create(schema, adminUser);
				log.info("Created schema container {" + schema.getName() + "} uuid: {" + folderSchemaContainer.getUuid() + "}");
			}

			// Binary content for images and other downloads
			SchemaContainer binarySchemaContainer = schemaContainerRoot.findByName("binary-content").toBlocking().value();
			if (binarySchemaContainer == null) {

				Schema schema = new SchemaModel();
				schema.setName("binary-content");
				schema.setDisplayField("name");
				schema.setSegmentField("binary");

				StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
				nameFieldSchema.setName("name");
				nameFieldSchema.setLabel("Name");
				schema.addField(nameFieldSchema);

				BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
				binaryFieldSchema.setName("binary");
				binaryFieldSchema.setLabel("Binary Data");
				schema.addField(binaryFieldSchema);

				schema.setContainer(false);
				binarySchemaContainer = schemaContainerRoot.create(schema, adminUser);
				log.info("Created schema container {" + schema.getName() + "} uuid: {" + binarySchemaContainer.getUuid() + "}");
			}

			Group adminGroup = groupRoot.findByName("admin").toBlocking().value();
			if (adminGroup == null) {
				adminGroup = groupRoot.create("admin", adminUser);
				adminGroup.addUser(adminUser);
				log.info("Created admin group {" + adminGroup.getUuid() + "}");
			}

			adminRole = roleRoot.findByName("admin").toBlocking().value();
			if (adminRole == null) {
				adminRole = roleRoot.create("admin", adminUser);
				adminGroup.addRole(adminRole);
				log.info("Created admin role {" + adminRole.getUuid() + "}");
			}

			LanguageRoot languageRoot = meshRoot.getLanguageRoot();
			initLanguages(languageRoot);

			schemaStorage.init();
			tx.success();
		}

	}

	/**
	 * Grant CRUD to all objects within the graph to the Admin Role.
	 */
	public void initPermissions() {
		try (Tx tx = db.tx()) {
			Role adminRole = meshRoot().getRoleRoot().findByName("admin").toBlocking().value();
			for (Vertex vertex : tx.getGraph().getVertices()) {
				WrappedVertex wrappedVertex = (WrappedVertex) vertex;
				MeshVertex meshVertex = tx.getGraph().frameElement(wrappedVertex.getBaseElement(), MeshVertexImpl.class);
				adminRole.grantPermissions(meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM);
				if (log.isTraceEnabled()) {
					log.trace("Granting admin CRUD permissions on vertex {" + meshVertex.getUuid() + "} for role {" + adminRole.getUuid() + "}");
				}
			}
			tx.success();
		}
	}

	/**
	 * Initialize the languages by loading the json file and creating the language graph elements.
	 * 
	 * @param root
	 *            Aggregation node to which the languages will be assigned
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	protected void initLanguages(LanguageRoot root) throws JsonParseException, JsonMappingException, IOException {

		long start = System.currentTimeMillis();
		final String filename = "languages.json";
		final InputStream ins = getClass().getResourceAsStream("/json/" + filename);
		if (ins == null) {
			throw new NullPointerException("Languages could not be loaded from classpath file {" + filename + "}");
		}
		LanguageSet languageSet = new ObjectMapper().readValue(ins, LanguageSet.class);
		for (Map.Entry<String, LanguageEntry> entry : languageSet.entrySet()) {
			String languageTag = entry.getKey();
			String languageName = entry.getValue().getName();
			String languageNativeName = entry.getValue().getNativeName();
			Language language = meshRoot().getLanguageRoot().findByName(languageName).toBlocking().value();
			if (language == null) {
				language = root.create(languageName, languageTag);
				language.setNativeName(languageNativeName);
				if (log.isDebugEnabled()) {
					log.debug("Added language {" + languageTag + " / " + languageName + "}");
				}
			}
		}
		long diff = System.currentTimeMillis() - start;
		log.info("Handling languages took: " + diff + "[ms]");
	}

}
