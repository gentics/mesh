package com.gentics.mesh.cli;

import static com.gentics.mesh.core.rest.MeshEvent.STARTUP;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.cache.CacheRegistry;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystem;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibMeshVersion;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.LanguageDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.service.ServerSchemaStorageImpl;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApiImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.job.JobType;
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
import com.gentics.mesh.etc.config.DebugInfoOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;
import com.gentics.mesh.event.EventBusStore;
import com.gentics.mesh.monitor.liveness.EventBusLivenessManager;
import com.gentics.mesh.monitor.liveness.LivenessManager;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.IndexHandlerRegistryImpl;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;
import com.gentics.mesh.util.MavenVersionNumber;
import com.hazelcast.internal.util.XmlUtil;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.cluster.ClusterManager;

/**
 * @see BootstrapInitializer
 */
public abstract class AbstractBootstrapInitializer implements BootstrapInitializer {

	private static Logger log = LoggerFactory.getLogger(BootstrapInitializer.class);

	/**
	 * Name of the global lock for executing the changelog in cluster mode
	 */
	public static final String GLOBAL_CHANGELOG_LOCK_KEY = "MESH_CHANGELOG_LOCK";

	private static final String ADMIN_USERNAME = "admin";

	protected final ServerSchemaStorageImpl schemaStorage;

	protected final Database db;

	protected final SearchProvider searchProvider;

	protected final BCryptPasswordEncoder encoder;

	protected final DistributedEventManager eventManager;

	protected final Lazy<IndexHandlerRegistryImpl> indexHandlerRegistry;

	protected final Lazy<CoreVerticleLoader> loader;

	protected final HighLevelChangelogSystem highlevelChangelogSystem;

	protected final CacheRegistry cacheRegistry;

	protected final MeshPluginManager pluginManager;

	protected final MeshOptions options;

	protected final RouterStorageRegistryImpl routerStorageRegistry;

	protected final MetricsOptions metricsOptions;

	protected final LocalConfigApiImpl localConfigApi;

	protected final BCryptPasswordEncoder passwordEncoder;

	protected final MasterElector coordinatorMasterElector;

	protected final LivenessManager liveness;

	protected final EventBusLivenessManager eventbusLiveness;

	protected final EventBusStore eventBusStore;

	// TODO: Changing the role name or deleting the role would cause code that utilizes this field to break.
	// This is however a rare case.
	protected HibRole anonymousRole;

	protected MeshImpl mesh;

	public boolean isInitialSetup = true;

	protected List<String> allLanguageTags = new ArrayList<>();

	protected Vertx vertx;

	protected String initialPasswordInfo;

	private ClusterManager clusterManager;

	protected AbstractBootstrapInitializer(ServerSchemaStorageImpl schemaStorage, Database db,
			SearchProvider searchProvider, BCryptPasswordEncoder encoder, DistributedEventManager eventManager,
			Lazy<IndexHandlerRegistryImpl> indexHandlerRegistry, Lazy<CoreVerticleLoader> loader,
			HighLevelChangelogSystem highlevelChangelogSystem, CacheRegistry cacheRegistry,
			MeshPluginManager pluginManager, MeshOptions options, RouterStorageRegistryImpl routerStorageRegistry,
			MetricsOptions metricsOptions, LocalConfigApiImpl localConfigApi, BCryptPasswordEncoder passwordEncoder,
			MasterElector coordinatorMasterElector, LivenessManager liveness, EventBusLivenessManager eventbusLiveness,
			EventBusStore eventBusStore) {
		this.schemaStorage = schemaStorage;
		this.db = db;
		this.searchProvider = searchProvider;
		this.encoder = encoder;
		this.eventManager = eventManager;
		this.indexHandlerRegistry = indexHandlerRegistry;
		this.loader = loader;
		this.highlevelChangelogSystem = highlevelChangelogSystem;
		this.cacheRegistry = cacheRegistry;
		this.pluginManager = pluginManager;
		this.options = options;
		this.routerStorageRegistry = routerStorageRegistry;
		this.metricsOptions = metricsOptions;
		this.localConfigApi = localConfigApi;
		this.passwordEncoder = passwordEncoder;
		this.coordinatorMasterElector = coordinatorMasterElector;
		this.liveness = liveness;
		this.eventbusLiveness = eventbusLiveness;
		this.eventBusStore = eventBusStore;
		clearReferences();
	}

	/**
	 * Get the core verticle loader
	 * @return loader
	 */
	public CoreVerticleLoader getCoreVerticleLoader() {
		return loader.get();
	}

	/**
	 * Initialize the local data or create the initial dataset if no local data could be found.
	 *
	 * @param flags
	 * @param configuration
	 *			Mesh configuration
	 * @param isJoiningCluster
	 *			Flag which indicates that the instance is joining the cluster. In those cases various checks must not be invoked.
	 * @return True if an empty installation was detected, false if existing data was found
	 * @throws Exception
	 */
	protected void initLocalData(PostProcessFlags flags, MeshOptions configuration, boolean isJoiningCluster) throws Exception {
		boolean isEmptyInstallation = isEmptyInstallation();
		if (isEmptyInstallation) {
			initDatabaseTypes();
			// Setup mandatory data (e.g.: mesh root, project root, user root etc., admin user/role/group)
			initMandatoryData(configuration);
			initBasicData(configuration);
			initOptionalLanguages(configuration);
			initOptionalData(isEmptyInstallation);
			// TODO Initialize types
			initPermissions();
			handleMeshVersion();

			// Mark all changelog entries as applied for new installations
			markChangelogApplied();
			if (!(searchProvider instanceof TrackingSearchProvider)) {
				flags.requireReindex();
			}
		} else {
			handleMeshVersion();
			initOptionalLanguages(configuration);
			if (!isJoiningCluster) {
				// Only execute the changelog if there are any elements in the graph
				invokeChangelog(flags);
			} else {
				invokeChangelogInCluster(flags, configuration);
			}
		}
	}

	@Override
	public void initBasicData(MeshOptions config) {
		db().tx(tx -> {
			UserDao userDao = tx.userDao();
			SchemaDao schemaDao = tx.schemaDao();
			GroupDao groupDao = tx.groupDao();
			RoleDao roleDao = tx.roleDao();

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
				if (StringUtils.isNotBlank(pw)) {
					StringBuffer sb = new StringBuffer();
					String hash = passwordEncoder.encode(pw);
					sb.append("-----------------------\n");
					sb.append("- Admin Login\n");
					sb.append("-----------------------\n");
					sb.append("- Username: admin\n");
					sb.append("- Password: " + pw + "\n");
					sb.append("-----------------------\n");
					// TODO figure out a way to avoid the encode call during test execution. This will otherwise slow down tests big time.
					tx.userDao().updatePasswordHash(adminUser, hash);
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

			HibGroup adminGroup = groupDao.findByName("admin");
			if (adminGroup == null) {
				adminGroup = groupDao.create("admin", adminUser);
				groupDao.addUser(adminGroup, adminUser);
				log.debug("Created admin group {" + adminGroup.getUuid() + "}");
			}

			HibRole adminRole = roleDao.findByName("admin");
			if (adminRole == null) {
				adminRole = roleDao.create("admin", adminUser);
				groupDao.addRole(adminGroup, adminRole);
				log.debug("Created admin role {" + adminRole.getUuid() + "}");
			}
			tx.success();
		});
	}

	@Override
	public void init(Mesh mesh, boolean forceResync, MeshOptions options, MeshCustomLoader<Vertx> verticleLoader) throws Exception {
		// since we use xerces, we need to set this to "true"
		System.setProperty(XmlUtil.SYSTEM_PROPERTY_IGNORE_XXE_PROTECTION_FAILURES, "true");

		this.mesh = (MeshImpl) mesh;
		PostProcessFlags flags = new PostProcessFlags(forceResync, false);

		boolean isClustered = options.getClusterOptions().isEnabled();
		boolean isInitMode = options.isInitClusterMode();

		options = prepareMeshOptions(options);

		addDebugInfoLogAppender(options);
		db().init(MeshVersion.getBuildInfo().getVersion(), "com.gentics.mesh.core.data");

		if (isClustered) {
			initCluster(options, flags, isInitMode);
		} else {
			initStandalone(options, flags, isInitMode);
			// No cluster mode - Just setup the connection pool and load or setup the local data
			db().setupConnectionPool();
			initVertx(options);
			searchProvider.init();
			searchProvider.start();
			pluginManager.init();
			initLocalData(flags, options, false);
		}

		eventManager.registerHandlers();
		handleLocalData(flags, options, verticleLoader);

		// Load existing plugins
		pluginManager.start();
		pluginManager.deployExistingPluginFiles().subscribe(() -> {
			// Finally fire the startup event and log that bootstrap has completed
			log.info("Sending startup completed event to {" + STARTUP + "}");
			vertx.eventBus().publish(STARTUP.address, true);
		}, e -> {
			log.error("Error at plugin files deployment", e);
		});

		if (initialPasswordInfo != null) {
			System.out.println(initialPasswordInfo);
		}

		eventbusLiveness.startRegularChecks();
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
				log.warn("Keyfile {" + keyFile + "} not found. Not loading keys.");
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
	 *			The remote host name or IP
	 * @return An IP of a local network adapter
	 */
	protected String getLocalIpForRoutedRemoteIP(String destination) {
		try {
			byte[] ipBytes = InetAddress.getByName(destination).getAddress();

			try (DatagramSocket datagramSocket = new DatagramSocket()) {
				datagramSocket.connect(InetAddress.getByAddress(ipBytes), 10002);
				return datagramSocket.getLocalAddress().getHostAddress();
			}
		} catch (Exception e) {
			log.error("Could not determine local ip ", e);
			return null;
		}
	}

	/**
	 * Create and initialize Vert.x
	 * 
	 * @param options
	 */
	public void initVertx(MeshOptions options) {
		VertxOptions vertxOptions = new VertxOptions();
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
		if (options.getClusterOptions().isEnabled()) {
			log.info("Creating clustered Vert.x instance");
			vertx = createClusteredVertx(options, vertxOptions);
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
		this.eventBusStore.setEventBus(vertx.eventBus());
	}

	/**
	 * Handle local data and prepare mesh API.
	 *
	 * @param flags
	 * @param configuration
	 * @param verticleLoader
	 * @throws Exception
	 */
	private void handleLocalData(PostProcessFlags flags, MeshOptions configuration, MeshCustomLoader<Vertx> verticleLoader) throws Exception {
		// Load the verticles
		List<String> initialProjects = db().tx(tx -> {
			return tx.projectDao().findAll().stream()
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

		boolean isSearchEnabled = configuration.getSearchOptions().getUrl() != null;

		// Invoke reindex as requested
		if (isSearchEnabled && flags.isReindex()) {
			createSearchIndicesAndMappings();
		}

		if (isSearchEnabled && (flags.isReindex() || flags.isResync())) {
			SyncEventHandler.invokeSync(vertx, null);
		}

		// Handle admin password reset
		String password = configuration.getAdminPassword();
		if (password != null) {
			// wait for DB being ready, then update/create the admin user
			doWithLock(GLOBAL_CHANGELOG_LOCK_KEY,
					"setting admin password", ensureAdminUser(password), 60 * 1000).subscribe();
		}

		registerEventHandlers();

		checkImageCacheMigrated();
	}

	@Deprecated
	private void checkImageCacheMigrated() throws IOException {
		Path imageCachePath = Path.of(options.getImageOptions().getImageCacheDirectory());
		if (Files.exists(imageCachePath) && Files.list(imageCachePath).filter(path -> path.getFileName().toString().length() == 8).count() > 0) {
			db().singleTx(tx -> {
				log.info("Image cache requires migration, triggering the corresponding Job.");
				BulkActionContext bac = tx.<CommonTx>unwrap().data().getOrCreateBulkActionContext();
				tx.jobDao().findAll().stream().filter(job -> job.getType() == JobType.imagecache).forEach(job -> {
					tx.jobDao().delete(job);
				});
				tx.jobDao().enqueueImageCacheMigration(tx.userDao().findByUsername("admin"));
				MeshEvent.triggerJobWorker(mesh);
				return bac;
			}).doOnSuccess(bac -> bac.process(true)).subscribe();
		}
	}

	/**
	 * Return a completable which will try to get a global lock with given name and will then execute the locked action
	 * @param lockName name of the lock to acquire
	 * @param description description of the locked action (for log output)
	 * @param lockedAction locked action
	 * @param timeout timeout for waiting for the lock
	 * @return completable
	 */
	protected Completable doWithLock(String lockName, String description, Completable lockedAction, long timeout) {
		return mesh.getRxVertx().sharedData().rxGetLockWithTimeout(lockName, timeout).toMaybe()
				.flatMapCompletable(lock -> {
					log.debug("Acquired lock for " + description);
					return lockedAction.doFinally(() -> {
						log.debug("Releasing lock for " + description);
						lock.release();
					});
				});
	}

	/**
	 * Completable which will ensure that the admin user exists, is flagged as "admin" and has the given password set.
	 * @param password admin password
	 * @return completable
	 */
	protected Completable ensureAdminUser(String password) {
		return Completable.defer(() -> {
			db().tx(tx -> {
				UserDao userDao = tx.userDao();
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
			return Completable.complete();
		});
	}

	@Override
	public void registerEventHandlers() {
		routerStorageRegistry.registerEventbus();
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
		db().tx(tx -> {
			HibMeshVersion meshVersion = tx.data().meshVersion();
			String graphVersion = meshVersion.getMeshVersion();

			// Check whether the information was already saved once. Otherwise set it.
			if (graphVersion == null) {
				if (log.isDebugEnabled()) {
					log.debug("Mesh version was not yet stored. Saving current version {" + currentVersion + "}");
				}
				meshVersion.setMeshVersion(currentVersion);
				graphVersion = currentVersion;
			}

			// Check whether the graph version is newer compared to the current runtime version
			MavenVersionNumber graph = MavenVersionNumber.parse(graphVersion);
			int diff = graph.compareTo(current);
			// SNAPSHOT -> RELEASE
			boolean isSnapshotUpgrade = diff == -1 && graph.compareTo(current, false) == 0 && graph.isSnapshot() && !current.isSnapshot();

			//TODO Shouldn't this be documented or clearly optionated?
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
				throw new RuntimeException("You are currently trying to run release version {" + currentVersion
						+ "} but your instance was last run using a snapshot version. {" + graphVersion
						+ "}. Running this version could cause unforseen errors.");
			}

			boolean isVersionDowngrade = diff >= 1;
			if (isVersionDowngrade) {
				// We need to check the database revision. If the stored database revision matches up the needed rev of this jar we can allow the downgrade.
				String jarRev = db().getDatabaseRevision();
				String dbRev = meshVersion.getDatabaseRevision();
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
		return db().isEmptyDatabase();
	}

	@Override
	public void createSearchIndicesAndMappings() {
		if (options.getSearchOptions().getUrl() != null) {
			// Clear the old indices and recreate them
			searchProvider.clear()
				.andThen(Observable.fromIterable(indexHandlerRegistry.get().getHandlers())
					.flatMapCompletable(IndexHandler::init))
				.blockingAwait();
		}
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
				anonymousRole = Tx.get().roleDao().findByName("anonymous");
			}
		}
		return anonymousRole;
	}

	/**
	 * Clear all stored references to main graph vertices.
	 */
	@Override
	public void clearReferences() {
		anonymousRole = null;
	}

	@Override
	public void initLanguages() throws JsonParseException, JsonMappingException, IOException {
		final String filename = "languages.json";
		final InputStream ins = getClass().getResourceAsStream("/json/" + filename);
		if (ins == null) {
			throw new NullPointerException("Languages could not be loaded from classpath file {" + filename + "}");
		}
		LanguageSet languageSet = new ObjectMapper().readValue(ins, LanguageSet.class);
		db().tx(tx -> { initLanguageSet(tx, languageSet); });
	}

	@Override
	public void initOptionalLanguages(MeshOptions configuration) {
		String languagesFilePath = configuration.getLanguagesFilePath();
		if (StringUtils.isNotEmpty(languagesFilePath)) {
			File languagesFile = new File(languagesFilePath);
			db().tx(tx -> {
				try {
					LanguageSet languageSet = new ObjectMapper().readValue(languagesFile, LanguageSet.class);
					initLanguageSet(tx, languageSet);
					tx.success();
				} catch (IOException e) {
					log.error("Error while initializing optional languages from {" + languagesFilePath + "}", e);
					tx.rollback();
				}
			});
		}
	}

	@Override
	public Collection<? extends String> getAllLanguageTags() {
		if (allLanguageTags.isEmpty()) {
			for (HibLanguage l : Tx.get().languageDao().findAll()) {
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

	@Override
	public void initOptionalData(boolean isEmptyInstallation) {

		// Only setup optional data for empty installations
		if (isEmptyInstallation) {
			db().tx(tx -> {
				UserDao userDao = tx.userDao();
				GroupDao groupDao = tx.groupDao();
				RoleDao roleDao = tx.roleDao();

				initOptionalData(tx, isEmptyInstallation);

				// Verify that an anonymous user exists
				HibUser anonymousUser = userDao.findByUsername("anonymous");
				if (anonymousUser == null) {
					anonymousUser = userDao.create("anonymous", anonymousUser);
					anonymousUser.setCreator(anonymousUser);
					anonymousUser.setCreationTimestamp();
					anonymousUser.setEditor(anonymousUser);
					anonymousUser.setLastEditedTimestamp();
					tx.userDao().updatePasswordHash(anonymousUser, null);
					log.debug("Created anonymous user {" + anonymousUser.getUuid() + "}");
				}

				HibGroup anonymousGroup = groupDao.findByName("anonymous");
				if (anonymousGroup == null) {
					anonymousGroup = groupDao.create("anonymous", anonymousUser);
					groupDao.addUser(anonymousGroup, anonymousUser);
					log.debug("Created anonymous group {" + anonymousGroup.getUuid() + "}");
				}

				anonymousRole = roleDao.findByName("anonymous");
				if (anonymousRole == null) {
					anonymousRole = roleDao.create("anonymous", anonymousUser);
					groupDao.addRole(anonymousGroup, anonymousRole);
					log.debug("Created anonymous role {" + anonymousRole.getUuid() + "}");
				}

				tx.success();
			});
		}
	}

	/**
	 * Create languages in the set, which do not exist yet
	 *
	 * @param root
	 *			language root
	 * @param languageSet
	 *			language set
	 */
	protected void initLanguageSet(LanguageDao root, LanguageSet languageSet) {
		for (Map.Entry<String, LanguageEntry> entry : languageSet.entrySet()) {
			String languageTag = entry.getKey();
			String languageName = entry.getValue().getName();
			String languageNativeName = entry.getValue().getNativeName();
			HibLanguage language = Tx.get().languageDao().findByLanguageTag(languageTag);
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
	public void initMandatoryData(MeshOptions config) throws Exception {
		db.tx(tx -> {
			initPermissionRoots(tx);
			initLanguages();
			schemaStorage.init();
			tx.success();
		});
	}

	/**
	 * Init permission storage.
	 * 
	 * @param tx transaction
	 */
	protected abstract void initPermissionRoots(Tx tx);

	/**
	 * Init languages from the data set.
	 *
	 * @param languageSet
	 */
	protected void initLanguageSet(Tx tx, LanguageSet languageSet) {
		initLanguageSet(tx.languageDao(), languageSet);
	}

	/**
	 * Init implementation specific optional data within the given transaction.
	 *
	 * @param tx
	 * @param isEmptyInstallation
	 */
	protected abstract void initOptionalData(Tx tx, boolean isEmptyInstallation);

	/**
	 * Create a clustered vert.x instance and block until the instance has been created.
	 *
	 * @param options
	 *			Mesh options
	 * @param vertxOptions
	 *			Vert.x options
	 */
	protected Vertx createClusteredVertx(MeshOptions options, VertxOptions vertxOptions) {
		clusterManager = db.clusterManager().getVertxClusterManager();
		vertxOptions.setClusterManager(clusterManager);
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
			return fut.get(getClusteredVertxInitializationTimeoutInSeconds(), SECONDS);
		} catch (Exception e) {
			throw new RuntimeException("Error while creating clusterd Vert.x instance", e);
		}
	}

	/**
	 * The timeout used when initializing a clustered vertx instance. If the instance is not available during the provided
	 * timeout, and exception will be thrown
	 * @return
	 */
	public int getClusteredVertxInitializationTimeoutInSeconds() {
		return 10;
	}

	/**
	 * Return the cluster manager
	 * @return
	 */
	protected ClusterManager getClusterManager() {
		return clusterManager;
	}

	/**
	 * Get the database instance
	 *
	 * @return
	 */
	protected abstract Database db();

	/**
	 * Init non-cluster Mesh instance.
	 *
	 * @param options
	 * @param flags
	 * @param isInitMode
	 * @throws Exception
	 */
	protected abstract void initStandalone(MeshOptions options, PostProcessFlags flags, boolean isInitMode) throws Exception;

	/**
	 * Init clustered Mesh instance.
	 *
	 * @param options
	 * @param flags
	 * @param isInitMode
	 * @throws Exception
	 */
	protected abstract void initCluster(MeshOptions options, PostProcessFlags flags, boolean isInitMode) throws Exception;
}
