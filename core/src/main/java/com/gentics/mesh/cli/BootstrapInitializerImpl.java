package com.gentics.mesh.cli;

import static com.gentics.mesh.Events.STARTUP_EVENT_ADDRESS;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.changelog.ChangelogSystem;
import com.gentics.mesh.changelog.ReindexAction;
import com.gentics.mesh.core.cache.PermissionStore;
import com.gentics.mesh.core.console.ConsoleProvider;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.binary.BinaryRoot;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.core.data.job.JobRoot;
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
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.distributed.DistributedEventManager;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.LanguageEntry;
import com.gentics.mesh.etc.LanguageSet;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.util.MavenVersionNumber;
import com.hazelcast.core.HazelcastInstance;
import com.syncleus.ferma.tx.Tx;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

import dagger.Lazy;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * @see BootstrapInitializer
 */
@Singleton
public class BootstrapInitializerImpl implements BootstrapInitializer {

	private static Logger log = LoggerFactory.getLogger(BootstrapInitializer.class);

	@Inject
	public ServerSchemaStorage schemaStorage;

	@Inject
	public Database db;

	@Inject
	public SearchProvider searchProvider;

	@Inject
	public BCryptPasswordEncoder encoder;

	@Inject
	public DistributedEventManager eventManager;

	@Inject
	public Lazy<IndexHandlerRegistry> indexHandlerRegistry;

	@Inject
	public Lazy<CoreVerticleLoader> loader;

	@Inject
	public ConsoleProvider console;

	private static MeshRoot meshRoot;

	private MeshImpl mesh;

	private HazelcastClusterManager manager;

	public static boolean isInitialSetup = true;

	private List<String> allLanguageTags = new ArrayList<>();

	private final ReindexAction REINDEX_ACTION = (() -> {

		// Init the classes / indices
		DatabaseHelper.init(db);

		// 1. Drop all indices
		log.info("Clearing all indices..");
		searchProvider.clear();
		log.info("Clearing indices completed.");

		// 2. Recreate indices + mappings and reindex the documents
		IndexHandlerRegistry registry = indexHandlerRegistry.get();
		for (IndexHandler<?> handler : registry.getHandlers()) {
			String handlerName = handler.getClass().getSimpleName();
			log.info("Invoking reindex on handler {" + handlerName + "}. This may take some time..");
			handler.init().blockingAwait();
			try (Tx tx = db.tx()) {
				handler.reindexAll().blockingAwait();
			}
			log.info("Reindex on handler {" + handlerName + "} completed.");
		}
	});

	@Inject
	public BootstrapInitializerImpl() {
		clearReferences();
	}

	/**
	 * The projects share various subrouters. This method will add the subrouters for all registered projects.
	 * 
	 * @throws InvalidNameException
	 */
	private void initProjects() throws InvalidNameException {
		for (Project project : meshRoot().getProjectRoot().findAllIt()) {
			RouterStorage.addProject(project.getName());
			if (log.isDebugEnabled()) {
				log.debug("Initalized project {" + project.getName() + "}");
			}
		}
	}

	/**
	 * Initialize the local data or create the initial dataset if no local data could be found.
	 * 
	 * @param isJoiningCluster
	 *            Flag which indicates that the instance is joining the cluster. In those cases various checks must not be invoked.
	 * @return True if an empty installation was detected, false if existing data was found
	 * @throws Exception
	 */
	private boolean initLocalData(boolean isJoiningCluster) throws Exception {
		boolean isEmptyInstallation = isEmptyInstallation();
		if (isEmptyInstallation) {
			// Update graph indices and vertex types (This may take some time)
			DatabaseHelper.init(db);
			// Setup mandatory data (e.g.: mesh root, project root, user root etc., admin user/role/group)
			initMandatoryData();
			initOptionalData(isEmptyInstallation);
			initPermissions();
			handleMeshVersion();

			// Mark all changelog entries as applied for new installations
			markChangelogApplied();
			return true;
		} else {

			handleMeshVersion();
			if (!isJoiningCluster) {
				// Only execute the changelog if there are any elements in the graph
				invokeChangelog();
				// Update graph indices and vertex types (This may take some time)
				DatabaseHelper.init(db);
			}
			return false;
		}
	}

	@Override
	public void init(Mesh mesh, boolean forceReindex, MeshOptions options, MeshCustomLoader<Vertx> verticleLoader) throws Exception {
		this.mesh = (MeshImpl) mesh;
		GraphStorageOptions storageOptions = options.getStorageOptions();
		boolean isClustered = options.getClusterOptions().isEnabled();
		boolean isInitMode = options.isInitClusterMode();
		boolean startOrientServer = storageOptions != null && storageOptions.getStartServer();

		try {
			db.init(Mesh.mesh().getOptions(), MeshVersion.getBuildInfo().getVersion(), "com.gentics.mesh.core.data");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (isClustered) {
			ClusterOptions clusterOptions = options.getClusterOptions();

			// Check whether we need to update the settings and use a determined local IP
			if (clusterOptions.getNetworkHost() == null) {
				String localIp = getLocalIpForRoutedRemoteIP("8.8.8.8");
				log.info("No networkHost setting was specified within the cluster settings. Using the determined IP {" + localIp + "}.");
				clusterOptions.setNetworkHost(localIp);
			}

			searchProvider.init(options);
			searchProvider.start();

			if (isInitMode) {
				log.info("Init cluster flag was found. Creating initial graph database now.");
				// We need to init the graph db before starting the OrientDB Server. Otherwise the database will not get picked up by the orientdb server which
				// handles the clustering.
				db.setupConnectionPool();
				boolean setupData = initLocalData(false);
				// db.closeConnectionPool();
				db.startServer();
				// db.setupConnectionPool();
				initVertx(options, isClustered);
				if (setupData) {
					createSearchIndicesAndMappings();
				}
			} else {
				// We need to wait for other nodes and receive the graphdb
				db.startServer();
				initVertx(options, isClustered);
				Mesh.mesh().setStatus(MeshStatus.WAITING_FOR_CLUSTER);
				db.joinCluster();
				isInitialSetup = false;
				db.setupConnectionPool();
				initLocalData(true);
			}
			boolean active = false;
			while (!active) {
				log.info("Waiting for hazelcast to become active");
				active = manager.getHazelcastInstance().getLifecycleService().isRunning();
				if (active) {
					break;
				}
				Thread.sleep(1000);
			}
		} else {

			searchProvider.init(options);
			searchProvider.start();

			initVertx(options, isClustered);
			// No cluster mode - Just setup the connection pool and load or setup the local data
			db.setupConnectionPool();
			initLocalData(false);
			if (startOrientServer) {
				db.startServer();
			}
		}

		eventManager.registerHandlers();
		handleLocalData(forceReindex, options, verticleLoader);
	}

	/**
	 * Returns the IP of the network interface that is used to communicate with the given remote host/IP. Let's say we want to reach 8.8.8.8, it would return
	 * the IP of the local network adapter that is routed into the Internet.
	 * 
	 * @param destination
	 *            The remote host name or IP
	 * @return An IP of a local network adapter
	 */
	protected String getLocalIpForRoutedRemoteIP(String destination) {
		try {
			byte[] ipBytes = InetAddress.getByName(destination).getAddress();

			try (DatagramSocket datagramSocket = new DatagramSocket()) {
				datagramSocket.connect(InetAddress.getByAddress(ipBytes), 0);
				return datagramSocket.getLocalAddress().getHostAddress();
			}
		} catch (Exception e) {
			log.error("Could not determine local ip ", e);
			return null;
		}
	}

	public void initVertx(MeshOptions options, boolean isClustered) {
		VertxOptions vertxOptions = new VertxOptions();
		vertxOptions.setClustered(options.getClusterOptions().isEnabled());
		vertxOptions.setWorkerPoolSize(options.getVertxOptions().getWorkerPoolSize());
		vertxOptions.setEventLoopPoolSize(options.getVertxOptions().getEventPoolSize());
		if (vertxOptions.isClustered()) {
			log.info("Creating clustered Vert.x instance");
			mesh.setVertx(createClusteredVertx(options, vertxOptions, (HazelcastInstance) db.getHazelcast()));
		} else {
			log.info("Creating non-clustered Vert.x instance");
			mesh.setVertx(Vertx.vertx(vertxOptions));
		}
	}

	/**
	 * Create a clustered vert.x instance and block until the instance has been created.
	 * 
	 * @param options
	 *            Mesh options
	 * @param vertxOptions
	 *            Vert.x options
	 * @param hazelcast
	 *            Hazelcast instance which should be used by vert.x
	 */
	private Vertx createClusteredVertx(MeshOptions options, VertxOptions vertxOptions, HazelcastInstance hazelcast) {
		Objects.requireNonNull(hazelcast, "The hazelcast instance was not yet initialized.");
		manager = new HazelcastClusterManager(hazelcast);
		vertxOptions.setClusterManager(manager);
		String localIp = options.getClusterOptions().getNetworkHost();
		vertxOptions.getEventBusOptions().setHost(localIp);
		vertxOptions.getEventBusOptions().setClusterPublicHost(localIp);
		vertxOptions.setClusterHost(localIp);
		vertxOptions.setClusterPublicHost(localIp);

		Integer clusterPort = options.getClusterOptions().getVertxPort();
		int vertxClusterPort = clusterPort == null ? 0 : clusterPort;
		vertxOptions.setClusterPort(vertxClusterPort);
		vertxOptions.setClusterPublicPort(vertxClusterPort);

		if (log.isDebugEnabled()) {
			log.debug("Using vert.x cluster port {" + vertxClusterPort + "}");
			log.debug("Using vert.x cluster public port {" + vertxClusterPort + "}");
			log.debug("Binding vert.x on host {" + localIp + "}");
		}
		CompletableFuture<Vertx> fut = new CompletableFuture<>();
		Vertx.clusteredVertx(vertxOptions, rh -> {
			log.info("Created clustered vert.x instance");
			if (rh.failed()) {
				Throwable cause = rh.cause();
				log.error("Failed to create clustered vert.x instance", cause);
				fut.completeExceptionally(new RuntimeException("Error while creating clusterd vert.x instance", cause));
				return;
			}
			Vertx vertx = rh.result();
			fut.complete(vertx);
		});
		try {
			return fut.get(10, SECONDS);
		} catch (Exception e) {
			throw new RuntimeException("Error while creating clusterd vert.x instance");
		}

	}

	/**
	 * Handle local data and prepare mesh API.
	 * 
	 * @param forceReindex
	 * @param configuration
	 * @param commandLine
	 * @param verticleLoader
	 * @throws Exception
	 */
	private void handleLocalData(boolean forceReindex, MeshOptions configuration, MeshCustomLoader<Vertx> verticleLoader) throws Exception {
		// Invoke reindex as requested
		if (forceReindex) {
			reindexAll();
		}

		// Load the verticles
		loader.get().loadVerticles(configuration);
		if (verticleLoader != null) {
			verticleLoader.apply(Mesh.vertx());
		}

		// Handle admin password reset
		String password = configuration.getAdminPassword();
		if (password != null) {
			try (Tx tx = db.tx()) {
				User adminUser = userRoot().findByName("admin");
				if (adminUser != null) {
					adminUser.setPassword(password);
				}
				tx.success();
			}
		}
		// Initialise routes for existing projects
		try (Tx tx = db.tx()) {
			initProjects();
		}
		registerEventHandlers();

		// Finally fire the startup event and log that bootstrap has completed
		log.info("Sending startup completed event to {" + STARTUP_EVENT_ADDRESS + "}");
		Mesh.vertx().eventBus().publish(STARTUP_EVENT_ADDRESS, true);

	}

	@Override
	public void registerEventHandlers() {
		RouterStorage.registerEventbus();
		PermissionStore.registerEventHandler();
	}

	@Override
	public void reindexAll() {
		REINDEX_ACTION.invoke();
	}

	@Override
	public void handleMeshVersion() {
		String currentVersion = Mesh.getPlainVersion();
		if (currentVersion.equals("Unknown")) {
			throw new RuntimeException("Current version could not be determined!");
		}

		MavenVersionNumber current = MavenVersionNumber.parse(currentVersion);
		if (current.isSnapshot()) {
			log.warn("You are running snapshot version {" + currentVersion
					+ "} of Gentics Mesh. Be aware that this version could potentially alter your instance in unexpected ways.");
		}
		try (Tx tx = db.tx()) {
			String graphVersion = meshRoot().getMeshVersion();

			// Check whether the information was already saved once. Otherwise set it.
			if (graphVersion == null) {
				if (log.isDebugEnabled()) {
					log.debug("Mesh version was not yet stored. Saving current version {" + currentVersion + "}");
				}
				meshRoot().setMeshVersion(currentVersion);
				graphVersion = currentVersion;
			}

			// Check whether the graph version is newer compared to the current runtime version
			MavenVersionNumber graph = MavenVersionNumber.parse(graphVersion);
			int diff = graph.compareTo(current);
			// SNAPSHOT -> RELEASE
			boolean isSnapshotUpgrade = diff == -1 && graph.compareTo(current, false) == 0 && graph.isSnapshot() && !current.isSnapshot();

			boolean ignoreSnapshotUpgrade = System.getProperty("ignoreSnapshotUpgradeCheck") != null;
			if (ignoreSnapshotUpgrade) {
				log.warn(
						"You disabled the upgrade check for snapshot upgrades. Please note that upgrading a snapshot version to a release version could create unforseen errors since the snapshot may have altered your data in a way which was not anticipated by the release.");
				log.warn("Press any key to continue. This warning will only be shown once.");
				try {
					console.read();
				} catch (IOException e) {
					throw new RuntimeException("Startup aborted", e);
				}
			}
			if (isSnapshotUpgrade && !ignoreSnapshotUpgrade) {
				log.error("You are currently trying to run release version {" + currentVersion
						+ "} but your instance was last run using a snapshot version. {" + graphVersion
						+ "}. Running this version could cause unforseen errors.");
				throw new RuntimeException("Downgrade not allowed");
			}

			boolean isVersionDowngrade = diff >= 1;
			if (isVersionDowngrade) {
				log.error("You are currently trying to run version {" + currentVersion + "} on a dump which was last used by version {" + graphVersion
						+ "}. This is not supported. You can't downgrade your mesh instance. Doing so would cause unforseen errors. Aborting startup.");
				throw new RuntimeException("Downgrade not allowed");
			}

			// Version is okay. So lets store it.
			meshRoot().setMeshVersion(currentVersion);
		}
	}

	@Override
	public boolean isEmptyInstallation() {
		try (Tx tx = db.tx()) {
			return !tx.getGraph().v().hasNext();
		}
	}

	@Override
	public void invokeChangelog() {
		log.info("Invoking database changelog check...");
		ChangelogSystem cls = new ChangelogSystem(db);
		if (!cls.applyChanges(REINDEX_ACTION)) {
			throw new RuntimeException("The changelog could not be applied successfully. See log above.");
		}
		log.info("Changelog completed.");
	}

	@Override
	public void markChangelogApplied() {
		log.info("This is the initial setup.. marking all found changelog entries as applied");
		ChangelogSystem cls = new ChangelogSystem(db);
		cls.markAllAsApplied();
	}

	@Override
	public void createSearchIndicesAndMappings() {
		IndexHandlerRegistry registry = indexHandlerRegistry.get();
		for (IndexHandler<?> handler : registry.getHandlers()) {
			handler.init().blockingAwait();
		}
	}

	/**
	 * Return the mesh root node. This method will also create the node if it could not be found within the graph.
	 * 
	 * @return
	 */
	@Override
	public MeshRoot meshRoot() {
		if (meshRoot == null) {
			synchronized (BootstrapInitializer.class) {
				// Check reference graph and finally create the node when it can't be found.
				Iterator<? extends MeshRootImpl> it = db.getVerticesForType(MeshRootImpl.class);
				if (it.hasNext()) {
					isInitialSetup = false;
					meshRoot = it.next();
				} else {
					meshRoot = Tx.getActive().getGraph().addFramedVertex(MeshRootImpl.class);
					if (log.isDebugEnabled()) {
						log.debug("Created mesh root {" + meshRoot.getUuid() + "}");
					}
				}
			}
		}
		return meshRoot;
	}

	@Override
	public SchemaContainerRoot schemaContainerRoot() {
		return meshRoot().getSchemaContainerRoot();
	}

	@Override
	public MicroschemaContainerRoot microschemaContainerRoot() {
		return meshRoot().getMicroschemaContainerRoot();
	}

	@Override
	public RoleRoot roleRoot() {
		return meshRoot().getRoleRoot();
	}

	@Override
	public TagRoot tagRoot() {
		return meshRoot().getTagRoot();
	}

	@Override
	public TagFamilyRoot tagFamilyRoot() {
		return meshRoot().getTagFamilyRoot();
	}

	@Override
	public NodeRoot nodeRoot() {
		return meshRoot().getNodeRoot();
	}

	@Override
	public BinaryRoot binaryRoot() {
		return meshRoot().getBinaryRoot();
	}

	@Override
	public UserRoot userRoot() {
		return meshRoot().getUserRoot();
	}

	@Override
	public GroupRoot groupRoot() {
		return meshRoot().getGroupRoot();
	}

	@Override
	public JobRoot jobRoot() {
		return meshRoot().getJobRoot();
	}

	@Override
	public LanguageRoot languageRoot() {
		return meshRoot().getLanguageRoot();
	}

	@Override
	public ProjectRoot projectRoot() {
		return meshRoot().getProjectRoot();
	}

	/**
	 * Clear all stored references to main graph vertices.
	 */
	public static void clearReferences() {
		BootstrapInitializerImpl.meshRoot = null;
		MeshRootImpl.clearReferences();
	}

	@Override
	public void initMandatoryData() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException {
		Role adminRole;
		MeshRoot meshRoot;

		try (Tx tx = db.tx()) {
			meshRoot = meshRoot();

			// Create the initial root vertices
			meshRoot.getNodeRoot();
			meshRoot.getTagRoot();
			meshRoot.getTagFamilyRoot();
			meshRoot.getProjectRoot();
			meshRoot.getLanguageRoot();
			meshRoot.getJobRoot();
			meshRoot.getBinaryRoot();

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
				SchemaModel schema = new SchemaModelImpl();
				schema.setName("content");
				schema.setDescription("Content schema for blogposts");
				schema.setDisplayField("title");
				schema.setSegmentField("slug");

				StringFieldSchema slugFieldSchema = new StringFieldSchemaImpl();
				slugFieldSchema.setName("slug");
				slugFieldSchema.setLabel("Slug");
				slugFieldSchema.setRequired(true);
				schema.addField(slugFieldSchema);

				StringFieldSchema titleFieldSchema = new StringFieldSchemaImpl();
				titleFieldSchema.setName("title");
				titleFieldSchema.setLabel("Title");
				schema.addField(titleFieldSchema);

				StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
				nameFieldSchema.setName("teaser");
				nameFieldSchema.setLabel("Teaser");
				nameFieldSchema.setRequired(true);
				schema.addField(nameFieldSchema);

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
				SchemaModel schema = new SchemaModelImpl();
				schema.setName("folder");
				schema.setDescription("Folder schema to create containers for other nodes.");
				schema.setDisplayField("name");
				schema.setSegmentField("slug");

				StringFieldSchema folderNameFieldSchema = new StringFieldSchemaImpl();
				folderNameFieldSchema.setName("slug");
				folderNameFieldSchema.setLabel("Slug");
				schema.addField(folderNameFieldSchema);

				StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
				nameFieldSchema.setName("name");
				nameFieldSchema.setLabel("Name");
				schema.addField(nameFieldSchema);

				schema.setContainer(true);
				folderSchemaContainer = schemaContainerRoot.create(schema, adminUser);
				log.debug("Created schema container {" + schema.getName() + "} uuid: {" + folderSchemaContainer.getUuid() + "}");
			}

			// Binary content for images and other downloads
			SchemaContainer binarySchemaContainer = schemaContainerRoot.findByName("binary_content");
			if (binarySchemaContainer == null) {

				SchemaModel schema = new SchemaModelImpl();
				schema.setDescription("Binary content schema used to store images and other binary data.");
				schema.setName("binary_content");
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

	@Override
	public void initOptionalData(boolean isEmptyInstallation) {

		// Only setup optional data for empty installations
		if (isEmptyInstallation) {
			try (Tx tx = db.tx()) {
				meshRoot = meshRoot();

				UserRoot userRoot = meshRoot().getUserRoot();
				// Verify that an anonymous user exists
				User anonymousUser = userRoot.findByUsername("anonymous");
				if (anonymousUser == null) {
					anonymousUser = userRoot.create("anonymous", anonymousUser);
					anonymousUser.setCreator(anonymousUser);
					anonymousUser.setCreationTimestamp();
					anonymousUser.setEditor(anonymousUser);
					anonymousUser.setLastEditedTimestamp();
					anonymousUser.setPasswordHash(null);
					log.debug("Created anonymous user {" + anonymousUser.getUuid() + "}");
				}

				GroupRoot groupRoot = meshRoot.getGroupRoot();
				Group anonymousGroup = groupRoot.findByName("anonymous");
				if (anonymousGroup == null) {
					anonymousGroup = groupRoot.create("anonymous", anonymousUser);
					anonymousGroup.addUser(anonymousUser);
					log.debug("Created anonymous group {" + anonymousGroup.getUuid() + "}");
				}

				RoleRoot roleRoot = meshRoot.getRoleRoot();
				Role anonymousRole = roleRoot.findByName("anonymous");
				if (anonymousRole == null) {
					anonymousRole = roleRoot.create("anonymous", anonymousUser);
					anonymousGroup.addRole(anonymousRole);
					log.debug("Created anonymous role {" + anonymousRole.getUuid() + "}");
				}

				tx.success();
			}
		}
	}

	@Override
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

	@Override
	public void initLanguages(LanguageRoot root) throws JsonParseException, JsonMappingException, IOException {
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

	}

	@Override
	public Collection<? extends String> getAllLanguageTags() {
		if (allLanguageTags.isEmpty()) {
			for (Language l : languageRoot().findAllIt()) {
				String tag = l.getLanguageTag();
				allLanguageTags.add(tag);
			}
		}
		return allLanguageTags;
	}

}
