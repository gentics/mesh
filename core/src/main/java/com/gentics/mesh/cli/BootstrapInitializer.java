package com.gentics.mesh.cli;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
import com.gentics.mesh.core.data.impl.DatabaseHelper;
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
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.LanguageEntry;
import com.gentics.mesh.etc.LanguageSet;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.index.IndexHandler;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

import dagger.Lazy;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * The bootstrap initialiser takes care of creating all mandatory graph elements for mesh. This includes the creation of MeshRoot, ProjectRoot, NodeRoot,
 * GroupRoot, UserRoot and various element such as the Admin User, Admin Group, Admin Role.
 */
@Singleton
public class BootstrapInitializer {

	private static Logger log = LoggerFactory.getLogger(BootstrapInitializer.class);

	private ServerSchemaStorage schemaStorage;

	private Database db;

	private BCryptPasswordEncoder encoder;

	private RouterStorage routerStorage;

	private Lazy<IndexHandlerRegistry> searchHandlerRegistry;

	private Lazy<CoreVerticleLoader> loader;

	private static MeshRoot meshRoot;

	public static boolean isInitialSetup = true;

	private List<String> allLanguageTags = new ArrayList<>();

	@Inject
	public BootstrapInitializer(Database db, Lazy<IndexHandlerRegistry> searchHandlerRegistry, BCryptPasswordEncoder encoder,
			RouterStorage routerStorage, Lazy<CoreVerticleLoader> loader) {

		clearReferences();

		this.db = db;
		this.searchHandlerRegistry = searchHandlerRegistry;
		this.schemaStorage = new ServerSchemaStorage(this);
		this.encoder = encoder;
		this.routerStorage = routerStorage;
		this.loader = loader;

	}

	/**
	 * The projects share various subrouters. This method will add the subrouters for all registered projects.
	 * 
	 * @throws InvalidNameException
	 */
	private void initProjects() throws InvalidNameException {
		for (Project project : meshRoot().getProjectRoot().findAll()) {
			routerStorage.addProjectRouter(project.getName());
			if (log.isDebugEnabled()) {
				log.debug("Initalized project {" + project.getName() + "}");
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
	 * Initialise mesh using the given configuration.
	 * 
	 * @param configuration
	 * @param verticleLoader
	 * @throws Exception
	 */
	public void init(MeshOptions configuration, MeshCustomLoader<Vertx> verticleLoader) throws Exception {
		if (configuration.isClusterMode()) {
			joinCluster();
		}

		// Only execute the installation if there are any elements in the graph
		boolean isEmptyInstallation = isEmptyInstallation();
		if (!isEmptyInstallation) {
			invokeChangelog();
		}

		new DatabaseHelper(db).init();

		initMandatoryData();
		if (isEmptyInstallation) {
			initPermissions();
		}

		// Mark all changelog entries as applied for new installations
		if (isEmptyInstallation) {
			markChangelogApplied();
		}

		// initPermissions();
		initSearchIndexHandlers();
		if (isEmptyInstallation) {
			createSearchIndicesAndMappings();
		}
		try {
			invokeSearchQueueProcessing();
		} catch (Exception e) {
			log.error("Could not handle existing search queue entries", e);
		}

		loader.get().loadVerticles(configuration);
		if (verticleLoader != null) {
			verticleLoader.apply(Mesh.vertx());
		}
		try (NoTx noTx = db.noTx()) {
			initProjects();
		}
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
	public void markChangelogApplied() {
		log.info("This is the initial setup.. marking all found changelog entries as applied");
		ChangelogSystem cls = new ChangelogSystem(db);
		cls.markAllAsApplied();
	}

	/**
	 * Initialise the search queue handlers.
	 */
	public void initSearchIndexHandlers() {
		IndexHandlerRegistry registry = searchHandlerRegistry.get();
		registry.init();
	}

	public void createSearchIndicesAndMappings() {
		IndexHandlerRegistry registry = searchHandlerRegistry.get();
		for (IndexHandler handler : registry.getHandlers()) {
			handler.init().await();
		}
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
					if (log.isDebugEnabled()) {
						log.debug("Created mesh root {" + meshRoot.getUuid() + "}");
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
				adminUser.setCreationTimestamp();
				adminUser.setEditor(adminUser);
				adminUser.setLastEditedTimestamp();

				log.debug("Enter admin password:");
				// Scanner scanIn = new Scanner(System.in);
				// String pw = scanIn.nextLine();
				// TODO remove later on
				String pw = "admin";
				// scanIn.close();
				adminUser.setPasswordHash(encoder.encode(pw));
				log.debug("Created admin user {" + adminUser.getUuid() + "}");
			}

			// Content
			SchemaContainer contentSchemaContainer = schemaContainerRoot.findByName("content");
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
				log.debug("Created schema container {" + schema.getName() + "} uuid: {" + contentSchemaContainer.getUuid() + "}");
			}

			// Folder
			SchemaContainer folderSchemaContainer = schemaContainerRoot.findByName("folder");
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
				log.debug("Created schema container {" + schema.getName() + "} uuid: {" + folderSchemaContainer.getUuid() + "}");
			}

			// Binary content for images and other downloads
			SchemaContainer binarySchemaContainer = schemaContainerRoot.findByName("binary-content");
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
				log.debug("Created schema container {" + schema.getName() + "} uuid: {" + binarySchemaContainer.getUuid() + "}");
			}

			Group adminGroup = groupRoot.findByName("admin");
			if (adminGroup == null) {
				adminGroup = groupRoot.create("admin", adminUser);
				adminGroup.addUser(adminUser);
				log.debug("Created admin group {" + adminGroup.getUuid() + "}");
			}

			adminRole = roleRoot.findByName("admin");
			if (adminRole == null) {
				adminRole = roleRoot.create("admin", adminUser);
				adminGroup.addRole(adminRole);
				log.debug("Created admin role {" + adminRole.getUuid() + "}");
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
			Role adminRole = meshRoot().getRoleRoot().findByName("admin");
			for (Vertex vertex : tx.getGraph().getVertices()) {
				WrappedVertex wrappedVertex = (WrappedVertex) vertex;
				MeshVertex meshVertex = tx.getGraph().frameElement(wrappedVertex.getBaseElement(), MeshVertexImpl.class);
				adminRole.grantPermissions(meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM, PUBLISH_PERM, READ_PUBLISHED_PERM);
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
			Language language = meshRoot().getLanguageRoot().findByName(languageName);
			if (language == null) {
				language = root.create(languageName, languageTag);
				language.setNativeName(languageNativeName);
				if (log.isDebugEnabled()) {
					log.debug("Added language {" + languageTag + " / " + languageName + "}");
				}
			}
		}
		long diff = System.currentTimeMillis() - start;
		log.debug("Handling languages took: " + diff + "[ms]");
	}

	public Collection<? extends String> getAllLanguageTags() {
		if (allLanguageTags.isEmpty()) {
			for (Language l : languageRoot().findAll()) {
				String tag = l.getLanguageTag();
				allLanguageTags.add(tag);
			}
		}
		return allLanguageTags;
	}

}
