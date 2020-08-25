package com.gentics.mesh.cli;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.STARTUP;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.cache.CacheRegistryImpl;
import com.gentics.mesh.changelog.ChangelogSystem;
import com.gentics.mesh.changelog.ReindexAction;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystem;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.changelog.ChangelogRoot;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.distributed.DistributedEventManager;
import com.gentics.mesh.distributed.coordinator.MasterElector;
import com.gentics.mesh.etc.LanguageEntry;
import com.gentics.mesh.etc.LanguageSet;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.DebugInfoOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.DevNullSearchProvider;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;
import com.gentics.mesh.util.MavenVersionNumber;
import com.gentics.mesh.util.RequirementsCheck;
import com.hazelcast.core.HazelcastInstance;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import dagger.Lazy;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * @see BootstrapInitializer
 */
@Singleton
public class BootstrapInitializerImpl implements BootstrapInitializer {

	private static Logger log = LoggerFactory.getLogger(BootstrapInitializer.class);

	private static final String ADMIN_USERNAME = "admin";

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
	public HighLevelChangelogSystem highlevelChangelogSystem;

	@Inject
	public CacheRegistryImpl cacheRegistry;

	@Inject
	public MeshPluginManager pluginManager;

	@Inject
	public MeshOptions options;

	@Inject
	public RouterStorageRegistryImpl routerStorageRegistry;

	@Inject
	public MetricsOptions metricsOptions;

	@Inject
	public LocalConfigApi localConfigApi;

	@Inject
	public BCryptPasswordEncoder passwordEncoder;

	@Inject
	public MasterElector coordinatorMasterElector;

	@Inject
	public DaoCollection daoCollection;

	private MeshRoot meshRoot;

	// TODO: Changing the role name or deleting the role would cause code that utilizes this field to break.
	// This is however a rare case.
	private HibRole anonymousRole;

	private MeshImpl mesh;

	private HazelcastClusterManager manager;

	public boolean isInitialSetup = true;

	private List<String> allLanguageTags = new ArrayList<>();

	private Vertx vertx;

	private String initialPasswordInfo;

	private final ReindexAction SYNC_INDEX_ACTION = (() -> {

		// Init the classes / indices
		DatabaseHelper.init(db);

		if (searchProvider instanceof TrackingSearchProvider) {
			return;
		}
		if (searchProvider instanceof DevNullSearchProvider) {
			return;
		}
		if (searchProvider.getClient() == null) {
			return;
		}
		// Ensure indices are setup and sync the documents
		log.info("Invoking index sync. This may take some time..");
		SyncEventHandler.invokeSyncCompletable(mesh()).blockingAwait();
		log.info("Index sync completed.");
	});

	@Inject
	public BootstrapInitializerImpl() {
		clearReferences();
	}

	/**
	 * Initialize the local data or create the initial dataset if no local data could be found.
	 *
	 * @param configuration
	 *            Mesh configuration
	 * @param isJoiningCluster
	 *            Flag which indicates that the instance is joining the cluster. In those cases various checks must not be invoked.
	 * @return True if an empty installation was detected, false if existing data was found
	 * @throws Exception
	 */
	private boolean initLocalData(MeshOptions configuration, boolean isJoiningCluster) throws Exception {
		boolean isEmptyInstallation = isEmptyInstallation();
		if (isEmptyInstallation) {
			// Update graph indices and vertex types (This may take some time)
			DatabaseHelper.init(db);
			// Setup mandatory data (e.g.: mesh root, project root, user root etc., admin user/role/group)
			initMandatoryData(configuration);
			initOptionalLanguages(configuration);
			initOptionalData(isEmptyInstallation);
			initPermissions();
			handleMeshVersion();

			// Mark all changelog entries as applied for new installations
			markChangelogApplied();
			return true;
		} else {
			handleMeshVersion();
			if (!isJoiningCluster) {
				initOptionalLanguages(configuration);
				// Only execute the changelog if there are any elements in the graph
				invokeChangelog();
			}
			return false;
		}
	}

	@Override
	public void init(Mesh mesh, boolean forceIndexSync, MeshOptions options, MeshCustomLoader<Vertx> verticleLoader) throws Exception {
		this.mesh = (MeshImpl) mesh;
		GraphStorageOptions storageOptions = options.getStorageOptions();
		boolean isClustered = options.getClusterOptions().isEnabled();
		boolean isInitMode = options.isInitClusterMode();
		boolean startOrientServer = storageOptions != null && storageOptions.getStartServer();

		options = prepareMeshOptions(options);

		addDebugInfoLogAppender(options);
		RequirementsCheck.init(storageOptions);
		try {
			db.init(MeshVersion.getBuildInfo().getVersion(), "com.gentics.mesh.core.data");
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

			// Ensure that hazelcast is started before Vert.x since it is required for Vert.x creation.
			db.clusterManager().startHazelcast();
			initVertx(options);

			if (isInitMode) {
				log.info("Init cluster flag was found. Creating initial graph database now.");
				// We need to init the graph db before starting the OrientDB Server. Otherwise the database will not get picked up by the orientdb server which
				// handles the clustering.
				db.setupConnectionPool();
				boolean setupData = initLocalData(options, false);
				db.closeConnectionPool();
				db.shutdown();

				db.clusterManager().start();
				db.clusterManager().registerEventHandlers();
				db.setupConnectionPool();
				searchProvider.init();
				searchProvider.start();
				if (setupData) {
					createSearchIndicesAndMappings();
				}
			} else {
				// We need to wait for other nodes and receive the graphdb
				db.clusterManager().start();
				db.clusterManager().registerEventHandlers();
				isInitialSetup = false;
				db.setupConnectionPool();
				searchProvider.init();
				searchProvider.start();
				initLocalData(options, true);
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
			coordinatorMasterElector.start();
		} else {
			initVertx(options);
			searchProvider.init();
			searchProvider.start();
			// No cluster mode - Just setup the connection pool and load or setup the local data
			db.setupConnectionPool();
			initLocalData(options, false);
			if (startOrientServer) {
				db.closeConnectionPool();
				db.clusterManager().start();
				db.setupConnectionPool();
			}
		}

		eventManager.registerHandlers();
		handleLocalData(forceIndexSync, options, verticleLoader);

		// Load existing plugins
		pluginManager.start();
		pluginManager.deployExistingPluginFiles().subscribe(() -> {
			// Finally fire the startup event and log that bootstrap has completed
			log.info("Sending startup completed event to {" + STARTUP + "}");
			vertx.eventBus().publish(STARTUP.address, true);
		}, log::error);

		if (initialPasswordInfo != null) {
			System.out.println(initialPasswordInfo);
		}
	}

	/**
	 * Adds a file appender to the logging system. The log file is later used by the
	 * {@link com.gentics.mesh.core.endpoint.admin.debuginfo.providers.LogProvider}
	 * 
	 * @param options
	 */
	private void addDebugInfoLogAppender(MeshOptions options) {
		DebugInfoOptions debugInfoOptions = options.getDebugInfoOptions();
		if (!debugInfoOptions.isLogEnabled()) {
			return;
		}
		String logFolder = debugInfoOptions.getLogFolder();
		// This requires that slf4j is actually used.
		LoggerContext lc = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
		ch.qos.logback.classic.Logger rootLogger = lc.getLogger(ROOT_LOGGER_NAME);

		RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
		appender.setFile(Paths.get(logFolder, "debuginfo.log").toString());
		appender.setContext(lc);

		SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
		triggeringPolicy.setMaxFileSize(FileSize.valueOf(debugInfoOptions.getLogFileSize()));
		triggeringPolicy.setContext(lc);

		FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();

		rollingPolicy.setMinIndex(1);
		rollingPolicy.setMaxIndex(1);
		rollingPolicy.setFileNamePattern(Paths.get(logFolder, "debuginfo.%i.log").toString());
		rollingPolicy.setParent(appender);
		rollingPolicy.setContext(lc);

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setPattern(debugInfoOptions.getLogPattern());
		encoder.setContext(lc);

		appender.setRollingPolicy(rollingPolicy);
		appender.setTriggeringPolicy(triggeringPolicy);
		appender.setEncoder(encoder);

		rootLogger.addAppender(appender);
		triggeringPolicy.start();
		rollingPolicy.start();
		encoder.start();
		appender.start();
	}

	/**
	 * Prepare the mesh options.
	 *
	 * @param options
	 * @return
	 */
	private MeshOptions prepareMeshOptions(MeshOptions options) {
		loadExtraPublicKeys(options);
		options.validate();
		return options;
	}

	/**
	 * Load the list of JWK's that may have been added to the public keys file.
	 *
	 * @param options
	 */
	private void loadExtraPublicKeys(MeshOptions options) {
		AuthenticationOptions auth = options.getAuthenticationOptions();
		String keyPath = auth.getPublicKeysPath();
		if (StringUtils.isNotEmpty(keyPath)) {
			File keyFile = new File(keyPath);
			if (keyFile.exists()) {
				try {
					String json = FileUtils.readFileToString(keyFile);
					JsonObject jwks = new JsonObject(json);
					JsonArray keys = jwks.getJsonArray("keys");
					if (keys == null) {
						throw new RuntimeException("The file {" + keyFile + "} did not contain an array with the name keys.");
					}
					for (int i = 0; i < keys.size(); i++) {
						auth.getPublicKeys().add(keys.getJsonObject(i));
					}
				} catch (IOException e) {
					throw new RuntimeException("Could not read {" + keyFile + "}");
				}
			} else {
				log.warn("Keyfile {" + keyFile + "} not found. Not loading keys..");
			}
		}
	}

	@Override
	public void globalCacheClear() {
		cacheRegistry.clear();
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

	public void initVertx(MeshOptions options) {
		VertxOptions vertxOptions = new VertxOptions();
		vertxOptions.getEventBusOptions().setClustered(options.getClusterOptions().isEnabled());
		vertxOptions.setWorkerPoolSize(options.getVertxOptions().getWorkerPoolSize());
		vertxOptions.setEventLoopPoolSize(options.getVertxOptions().getEventPoolSize());

		MonitoringConfig monitoringOptions = options.getMonitoringOptions();
		if (monitoringOptions != null && monitoringOptions.isEnabled()) {
			log.info("Enabling Vert.x metrics");
			vertxOptions.setMetricsOptions(metricsOptions);
		}
		boolean logActivity = LoggerFactory.getLogger(EventBus.class).isDebugEnabled();
		vertxOptions.getEventBusOptions().setLogActivity(logActivity);
		vertxOptions.setPreferNativeTransport(true);
		System.setProperty("vertx.cacheDirBase", options.getTempDirectory());
		Vertx vertx = null;
		if (vertxOptions.getEventBusOptions().isClustered()) {
			log.info("Creating clustered Vert.x instance");
			vertx = createClusteredVertx(options, vertxOptions, (HazelcastInstance) db.clusterManager().getHazelcast());
		} else {
			log.info("Creating non-clustered Vert.x instance");
			vertx = Vertx.vertx(vertxOptions);
		}
		if (vertx.isNativeTransportEnabled()) {
			log.info("Running with native transports enabled");
		} else {
			log.warn("Current environment does not support native transports");
		}

		this.vertx = vertx;
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

		Integer clusterPort = options.getClusterOptions().getVertxPort();
		int vertxClusterPort = clusterPort == null ? 0 : clusterPort;

		EventBusOptions eventbus = vertxOptions.getEventBusOptions();
		eventbus.setHost(localIp);
		eventbus.setPort(vertxClusterPort);
		eventbus.setClusterPublicHost(localIp);
		eventbus.setClusterPublicPort(vertxClusterPort);

		if (log.isDebugEnabled()) {
			log.debug("Using Vert.x cluster port {" + vertxClusterPort + "}");
			log.debug("Using Vert.x cluster public port {" + vertxClusterPort + "}");
			log.debug("Binding Vert.x on host {" + localIp + "}");
		}
		CompletableFuture<Vertx> fut = new CompletableFuture<>();
		Vertx.clusteredVertx(vertxOptions, rh -> {
			log.info("Created clustered Vert.x instance");
			if (rh.failed()) {
				Throwable cause = rh.cause();
				log.error("Failed to create clustered Vert.x instance", cause);
				fut.completeExceptionally(new RuntimeException("Error while creating clusterd Vert.x instance", cause));
				return;
			}
			Vertx vertx = rh.result();
			fut.complete(vertx);
		});
		try {
			return fut.get(10, SECONDS);
		} catch (Exception e) {
			throw new RuntimeException("Error while creating clusterd Vert.x instance");
		}

	}

	/**
	 * Handle local data and prepare mesh API.
	 *
	 * @param forceIndexSync
	 * @param configuration
	 * @param verticleLoader
	 * @throws Exception
	 */
	private void handleLocalData(boolean forceIndexSync, MeshOptions configuration, MeshCustomLoader<Vertx> verticleLoader) throws Exception {
		// Load the verticles
		List<String> initialProjects = db.tx(tx -> {
			return tx.data().projectDao().findAll().stream()
				.map(HibProject::getName)
				.collect(Collectors.toList());
		});

		// Set read only mode
		localConfigApi.init()
			.andThen(loader.get().loadVerticles(initialProjects))
			.blockingAwait();

		if (verticleLoader != null) {
			verticleLoader.apply(vertx);
		}

		// Invoke reindex as requested
		if (forceIndexSync) {
			SyncEventHandler.invokeSync(vertx);
		}

		// Handle admin password reset
		String password = configuration.getAdminPassword();
		if (password != null) {
			db.tx(tx -> {
				UserDaoWrapper userDao = tx.data().userDao();
				HibUser adminUser = userDao.findByName(ADMIN_USERNAME);
				if (adminUser != null) {
					userDao.setPassword(adminUser, password);
					adminUser.setAdmin(true);
				} else {
					// Recreate the user if it can't be found.
					adminUser = userDao.create(ADMIN_USERNAME, null);
					adminUser.setCreator(adminUser);
					adminUser.setCreationTimestamp();
					adminUser.setEditor(adminUser);
					adminUser.setLastEditedTimestamp();
					userDao.setPassword(adminUser, password);
					adminUser.setAdmin(true);
				}
				tx.success();
			});
		}

		registerEventHandlers();

	}

	@Override
	public void registerEventHandlers() {
		routerStorageRegistry.registerEventbus();
	}

	@Override
	public void syncIndex() {
		SYNC_INDEX_ACTION.invoke();
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
		db.tx(tx -> {
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
					System.in.read();
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
				// We need to check the database revision. If the stored database revision matches up the needed rev of this jar we can allow the downgrade.
				String jarRev = db.getDatabaseRevision();
				String dbRev = meshRoot().getDatabaseRevision();
				if (dbRev != null && jarRev.equals(dbRev)) {
					log.info("Downgrade allowed since the database revision of {" + dbRev + "} matches the needed revision.");
				} else {
					log.error("You are currently trying to run version {" + currentVersion + "} on a dump which was last used by version {"
						+ graphVersion
						+ "}. This is not supported. You can't downgrade your mesh instance. Doing so would cause unforseen errors. Aborting startup.");
					if (dbRev != null) {
						throw new RuntimeException(
							"Downgrade not allowed since the database rev of {" + dbRev + "} does not match the needed rev {" + jarRev + "}");
					} else {
						throw new RuntimeException("Downgrade not allowed since the database is pre-revision handling.");
					}
				}
			}
		});
	}

	@Override
	public boolean isEmptyInstallation() {
		return db.tx(tx -> !tx.getGraph().v().hasNext());
	}

	@Override
	public void invokeChangelog() {
		log.info("Invoking database changelog check...");
		ChangelogSystem cls = new ChangelogSystem(db, options);
		if (!cls.applyChanges(SYNC_INDEX_ACTION)) {
			throw new RuntimeException("The changelog could not be applied successfully. See log above.");
		}

		// Update graph indices and vertex types (This may take some time)
		DatabaseHelper.init(db);

		// Now run the high level changelog entries
		highlevelChangelogSystem.apply(meshRoot);

		log.info("Changelog completed.");
		cls.setCurrentVersionAndRev();
	}

	@Override
	public void markChangelogApplied() {
		log.info("This is the initial setup.. marking all found changelog entries as applied");
		ChangelogSystem cls = new ChangelogSystem(db, options);
		cls.markAllAsApplied();
		highlevelChangelogSystem.markAllAsApplied(meshRoot);
		log.info("All changes marked");
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
					meshRoot = Tx.get().getGraph().addFramedVertex(MeshRootImpl.class);
					if (log.isDebugEnabled()) {
						log.debug("Created mesh root {" + meshRoot.getUuid() + "}");
					}
				}
			}
		}
		return meshRoot;
	}

	/**
	 * Return the anonymous role.
	 *
	 * @return
	 */
	@Override
	public HibRole anonymousRole() {
		if (anonymousRole == null) {
			synchronized (BootstrapInitializer.class) {
				// Load the role if it has not been yet loaded
				anonymousRole = roleRoot().findByName("anonymous");
			}
		}
		return anonymousRole;
	}

	@Override
	public SchemaRoot schemaContainerRoot() {
		return meshRoot().getSchemaContainerRoot();
	}

	@Override
	public SchemaDaoWrapper schemaDao() {
		return daoCollection.schemaDao();
	}

	@Override
	public MicroschemaRoot microschemaContainerRoot() {
		return meshRoot().getMicroschemaContainerRoot();
	}

	@Override
	public MicroschemaDaoWrapper microschemaDao() {
		return daoCollection.microschemaDao();
	}

	@Override
	public RoleRoot roleRoot() {
		return meshRoot().getRoleRoot();
	}

	@Override
	public RoleDaoWrapper roleDao() {
		return daoCollection.roleDao();
	}

	@Override
	public TagRoot tagRoot() {
		return meshRoot().getTagRoot();
	}

	@Override
	public TagDaoWrapper tagDao() {
		return daoCollection.tagDao();
	}

	@Override
	public TagFamilyRoot tagFamilyRoot() {
		return meshRoot().getTagFamilyRoot();
	}

	@Override
	public TagFamilyDaoWrapper tagFamilyDao() {
		return daoCollection.tagFamilyDao();
	}

	@Override
	public ChangelogRoot changelogRoot() {
		return meshRoot().getChangelogRoot();
	}

	@Override
	public LanguageDaoWrapper languageDao() {
		return daoCollection.languageDao();
	}

	@Override
	public UserRoot userRoot() {
		return meshRoot().getUserRoot();
	}

	@Override
	public UserDaoWrapper userDao() {
		return daoCollection.userDao();
	}

	@Override
	public GroupRoot groupRoot() {
		return meshRoot().getGroupRoot();
	}

	@Override
	public GroupDaoWrapper groupDao() {
		return daoCollection.groupDao();
	}

	@Override
	public JobRoot jobRoot() {
		return meshRoot().getJobRoot();
	}

	@Override
	public JobDaoWrapper jobDao() {
		return daoCollection.jobDao();
	}

	@Override
	public LanguageRoot languageRoot() {
		return meshRoot().getLanguageRoot();
	}

	@Override
	public ProjectRoot projectRoot() {
		return meshRoot().getProjectRoot();
	}

	@Override
	public ProjectDaoWrapper projectDao() {
		return daoCollection.projectDao();
	}

	@Override
	public NodeDaoWrapper nodeDao() {
		return daoCollection.nodeDao();
	}

	@Override
	public ContentDaoWrapper contentDao() {
		return daoCollection.contentDao();
	}

	/**
	 * Clear all stored references to main graph vertices.
	 */
	@Override
	public void clearReferences() {
		if (meshRoot != null) {
			meshRoot.clearReferences();
		}
		meshRoot = null;
		anonymousRole = null;
	}

	@Override
	public void initMandatoryData(MeshOptions config) throws Exception {

		db.tx(tx -> {
			UserDaoWrapper userDao = tx.data().userDao();
			GroupDaoWrapper groupDao = tx.data().groupDao();
			RoleDaoWrapper roleDao = tx.data().roleDao();

			MeshRoot meshRoot = meshRoot();

			// Create the initial root vertices
			meshRoot.getTagRoot();
			meshRoot.getTagFamilyRoot();
			meshRoot.getProjectRoot();
			meshRoot.getLanguageRoot();
			meshRoot.getJobRoot();
			meshRoot.getChangelogRoot();

			GroupRoot groupRoot = meshRoot.getGroupRoot();
			RoleRoot roleRoot = meshRoot.getRoleRoot();
			SchemaDaoWrapper schemaDao = tx.data().schemaDao();

			// Verify that an admin user exists
			HibUser adminUser = userDao.findByUsername(ADMIN_USERNAME);
			if (adminUser == null) {
				adminUser = userDao.create(ADMIN_USERNAME, adminUser);
				adminUser.setAdmin(true);
				adminUser.setCreator(adminUser);
				adminUser.setCreationTimestamp();
				adminUser.setEditor(adminUser);
				adminUser.setLastEditedTimestamp();

				String pw = config.getInitialAdminPassword();
				if (pw != null) {
					StringBuffer sb = new StringBuffer();
					String hash = passwordEncoder.encode(pw);
					sb.append("-----------------------\n");
					sb.append("- Admin Login\n");
					sb.append("-----------------------\n");
					sb.append("- Username: admin\n");
					sb.append("- Password: " + pw + "\n");
					sb.append("-----------------------\n");
					// TODO figure out a way to avoid the encode call during test execution. This will otherwise slow down tests big time.
					adminUser.setPasswordHash(hash);
					if (config.isForceInitialAdminPasswordReset()) {
						sb.append("- Password reset forced on initial login.\n");
						adminUser.setForcedPasswordChange(true);
						sb.append("-----------------------\n");
					}
					initialPasswordInfo = sb.toString();
				} else {
					log.warn("No initial password specified. Creating admin user without password!");
				}
				log.debug("Created admin user {" + adminUser.getUuid() + "}");
			}

			// Content
			HibSchema contentSchemaContainer = schemaDao.findByName("content");
			if (contentSchemaContainer == null) {
				SchemaVersionModel schema = new SchemaModelImpl();
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
				contentSchemaContainer = schemaDao.create(schema, adminUser, null, false);
				log.debug("Created schema container {" + schema.getName() + "} uuid: {" + contentSchemaContainer.getUuid() + "}");
			}

			// Folder
			HibSchema folderSchemaContainer = schemaDao.findByName("folder");
			if (folderSchemaContainer == null) {
				SchemaVersionModel schema = new SchemaModelImpl();
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
				folderSchemaContainer = schemaDao.create(schema, adminUser, null, false);
				log.debug("Created schema container {" + schema.getName() + "} uuid: {" + folderSchemaContainer.getUuid() + "}");
			}

			// Binary content for images and other downloads
			HibSchema binarySchemaContainer = schemaDao.findByName("binary_content");
			if (binarySchemaContainer == null) {

				SchemaVersionModel schema = new SchemaModelImpl();
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
				binarySchemaContainer = schemaDao.create(schema, adminUser, null, false);
				log.debug("Created schema container {" + schema.getName() + "} uuid: {" + binarySchemaContainer.getUuid() + "}");
			}

			HibGroup adminGroup = groupRoot.findByName("admin");
			if (adminGroup == null) {
				adminGroup = groupDao.create("admin", adminUser);
				groupDao.addUser(adminGroup, adminUser);
				log.debug("Created admin group {" + adminGroup.getUuid() + "}");
			}

			HibRole adminRole = roleRoot.findByName("admin");
			if (adminRole == null) {
				adminRole = roleDao.create("admin", adminUser);
				groupDao.addRole(adminGroup, adminRole);
				log.debug("Created admin role {" + adminRole.getUuid() + "}");
			}

			LanguageRoot languageRoot = meshRoot.getLanguageRoot();
			initLanguages(languageRoot);

			schemaStorage.init();
			tx.success();
		});

	}

	@Override
	public void initOptionalData(boolean isEmptyInstallation) {

		// Only setup optional data for empty installations
		if (isEmptyInstallation) {
			db.tx(tx -> {
				UserDaoWrapper userDao = tx.data().userDao();
				GroupDaoWrapper groupDao = tx.data().groupDao();
				RoleDaoWrapper roleDao = tx.data().roleDao();

				meshRoot = meshRoot();

				// Verify that an anonymous user exists
				HibUser anonymousUser = userDao.findByUsername("anonymous");
				if (anonymousUser == null) {
					anonymousUser = userDao.create("anonymous", anonymousUser);
					anonymousUser.setCreator(anonymousUser);
					anonymousUser.setCreationTimestamp();
					anonymousUser.setEditor(anonymousUser);
					anonymousUser.setLastEditedTimestamp();
					anonymousUser.setPasswordHash(null);
					log.debug("Created anonymous user {" + anonymousUser.getUuid() + "}");
				}

				GroupRoot groupRoot = meshRoot.getGroupRoot();
				HibGroup anonymousGroup = groupRoot.findByName("anonymous");
				if (anonymousGroup == null) {
					anonymousGroup = groupDao.create("anonymous", anonymousUser);
					groupDao.addUser(anonymousGroup, anonymousUser);
					log.debug("Created anonymous group {" + anonymousGroup.getUuid() + "}");
				}

				RoleRoot roleRoot = meshRoot.getRoleRoot();
				anonymousRole = roleRoot.findByName("anonymous");
				if (anonymousRole == null) {
					anonymousRole = roleDao.create("anonymous", anonymousUser);
					groupDao.addRole(anonymousGroup, anonymousRole);
					log.debug("Created anonymous role {" + anonymousRole.getUuid() + "}");
				}

				tx.success();
			});
		}
	}

	@Override
	public void initPermissions() {
		db.tx(tx -> {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			Role adminRole = meshRoot().getRoleRoot().findByName("admin");
			for (Vertex vertex : tx.getGraph().getVertices()) {
				WrappedVertex wrappedVertex = (WrappedVertex) vertex;
				MeshVertex meshVertex = tx.getGraph().frameElement(wrappedVertex.getBaseElement(), MeshVertexImpl.class);
				roleDao.grantPermissions(adminRole, meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM, PUBLISH_PERM, READ_PUBLISHED_PERM);
				if (log.isTraceEnabled()) {
					log.trace("Granting admin CRUD permissions on vertex {" + meshVertex.getUuid() + "} for role {" + adminRole.getUuid() + "}");
				}
			}
			tx.success();
		});
	}

	@Override
	public void initLanguages(LanguageRoot root) throws JsonParseException, JsonMappingException, IOException {
		final String filename = "languages.json";
		final InputStream ins = getClass().getResourceAsStream("/json/" + filename);
		if (ins == null) {
			throw new NullPointerException("Languages could not be loaded from classpath file {" + filename + "}");
		}
		LanguageSet languageSet = new ObjectMapper().readValue(ins, LanguageSet.class);
		initLanguages(root, languageSet);

	}

	@Override
	public void initOptionalLanguages(MeshOptions configuration) {
		String languagesFilePath = configuration.getLanguagesFilePath();
		if (StringUtils.isNotEmpty(languagesFilePath)) {
			File languagesFile = new File(languagesFilePath);
			db.tx(tx -> {
				try {
					LanguageSet languageSet = new ObjectMapper().readValue(languagesFile, LanguageSet.class);
					initLanguages(meshRoot().getLanguageRoot(), languageSet);
					tx.success();
				} catch (IOException e) {
					log.error("Error while initializing optional languages from {" + languagesFilePath + "}", e);
					tx.rollback();
				}
			});
		}
	}

	/**
	 * Create languages in the set, which do not exist yet
	 *
	 * @param root
	 *            language root
	 * @param languageSet
	 *            language set
	 */
	protected void initLanguages(LanguageRoot root, LanguageSet languageSet) {
		for (Map.Entry<String, LanguageEntry> entry : languageSet.entrySet()) {
			String languageTag = entry.getKey();
			String languageName = entry.getValue().getName();
			String languageNativeName = entry.getValue().getNativeName();
			Language language = meshRoot().getLanguageRoot().findByLanguageTag(languageTag);
			if (language == null) {
				language = root.create(languageName, languageTag);
				language.setNativeName(languageNativeName);
				if (log.isDebugEnabled()) {
					log.debug("Added language {" + languageTag + " / " + languageName + "}");
				}
			} else {
				if (!StringUtils.equals(language.getName(), languageName)) {
					language.setName(languageName);
					if (log.isDebugEnabled()) {
						log.debug("Changed name of language {" + languageTag + " } to {" + languageName + "}");
					}
				}
				if (!StringUtils.equals(language.getNativeName(), languageNativeName)) {
					language.setNativeName(languageNativeName);
					if (log.isDebugEnabled()) {
						log.debug("Changed nativeName of language {" + languageTag + " } to {" + languageNativeName + "}");
					}
				}
			}
		}
	}

	@Override
	public Collection<? extends String> getAllLanguageTags() {
		if (allLanguageTags.isEmpty()) {
			for (Language l : languageRoot().findAll()) {
				String tag = l.getLanguageTag();
				allLanguageTags.add(tag);
			}
		}
		return allLanguageTags;
	}

	@Override
	public Vertx vertx() {
		return vertx;
	}

	@Override
	public Mesh mesh() {
		return mesh;
	}

	@Override
	public boolean isInitialSetup() {
		return isInitialSetup;
	}

	@Override
	public boolean isVertxReady() {
		return vertx != null;
	}
}
