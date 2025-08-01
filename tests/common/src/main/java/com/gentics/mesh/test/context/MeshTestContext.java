package com.gentics.mesh.test.context;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.MeshVersion.CURRENT_API_VERSION;
import static com.gentics.mesh.test.ElasticsearchTestMode.UNREACHABLE;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOExceptionList;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.ToxiproxyContainer.ContainerProxy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.util.KeycloakUtils;
import com.gentics.mesh.cli.AbstractBootstrapInitializer;
import com.gentics.mesh.cli.MeshImpl;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.crypto.KeyStoreHelper;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.module.SearchProviderModule;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;
import com.gentics.mesh.etc.config.S3CacheOptions;
import com.gentics.mesh.etc.config.S3Options;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.monitoring.MonitoringClientConfig;
import com.gentics.mesh.rest.monitoring.MonitoringRestClient;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.TrackingSearchProviderImpl;
import com.gentics.mesh.search.verticle.ElasticsearchProcessVerticle;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshInstanceProvider;
import com.gentics.mesh.test.MeshTestActions;
import com.gentics.mesh.test.MeshTestContextProvider;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.SSLTestMode;
import com.gentics.mesh.test.TestDataProvider;
import com.gentics.mesh.test.docker.AWSContainer;
import com.gentics.mesh.test.docker.ElasticsearchContainer;
import com.gentics.mesh.test.docker.KeycloakContainer;
import com.gentics.mesh.test.util.MeshAssert;
import com.gentics.mesh.test.util.TestUtils;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.json.JsonObject;
import okhttp3.OkHttpClient;

public class MeshTestContext implements TestRule {

	public static final String UNREACHABLE_HOST = "http://localhost:1";

	static {
		System.setProperty(TrackingSearchProviderImpl.TEST_PROPERTY_KEY, "true");
		System.setProperty("org.jboss.logging.provider", "slf4j");
	}

	public static final Logger LOG = LoggerFactory.getLogger(MeshTestContext.class);

	private static final String CONF_PATH = "target/config-" + System.currentTimeMillis();

	public static ElasticsearchContainer elasticsearch;

	public static KeycloakContainer keycloak;

	public static Network network;

	public static ToxiproxyContainer toxiproxy;

	public static ContainerProxy proxy;

	public static OkHttpClient okHttp = createTestClient();

	private final static AtomicInteger clusterNodeCounter = new AtomicInteger();

	private List<File> tmpFolders = new ArrayList<>();
	private TestDataProvider dataProvider;

	private List<String> deploymentIds = new ArrayList<>();

	private CountDownLatch idleLatch;
	private MessageConsumer<Object> idleConsumer;

	private MeshTestContextProvider meshTestContextProvider;

	private boolean needsSetup = true;

	private int lastDbHash;

	private int currentDbHash;

	private List<MeshTestInstance> instances = new ArrayList<>();

	public List<MeshTestInstance> getInstances() {
		return instances;
	}

	public Statement apply(final Statement base, final Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				starting(description);
				try {
					base.evaluate();
				} finally {
					finished(description);
				}
			}
		};
	}

	/**
	 * Start the test context
	 * @param description
	 */
	protected void starting(Description description) throws Throwable {
		MeshTestSetting settings = getSettings(description);
		// Setup the dagger context, orm, es once
		if (description.isSuite()) {
			setupOnce(settings);
		} else {
			setup(settings);
		}
	}

	public void setup(MeshTestSetting settings) throws Exception {
		// when the database needs to be reset when the hash changes, we compare the hashes now
		if (settings.resetBetweenTests() == ResetTestDb.ON_HASH_CHANGE && lastDbHash != currentDbHash) {
			// hash changed, so we need setup
			needsSetup = true;
			// and will force tearDown now
			tearDown(settings, true);
			lastDbHash = currentDbHash;
		}

		if (!needsSetup) {
			// database has already been setup, so omit this step
			return;
		}
		meshTestContextProvider.getInstanceProvider().initMeshData(settings, instances.get(0).meshDagger);
		initFolders(instances.get(0).mesh.getOptions());
		listenToSearchIdleEvent();
		// Set up the index definition of data independent entities.
		switch (settings.elasticsearch()) {
		case CONTAINER_ES6:
		case CONTAINER_ES7:
		case CONTAINER_ES8:
			setupIndexHandlers(true);
			break;
		default:
			break;
		}

		boolean setAdminPassword = settings.optionChanger() != MeshCoreOptionChanger.INITIAL_ADMIN_PASSWORD;
		setupData(instances.get(0).mesh.getOptions(), setAdminPassword);

		// Set up the index definition of data dependent entities.
		switch (settings.elasticsearch()) {
		case CONTAINER_ES6:
		case CONTAINER_ES7:
		case CONTAINER_ES8:
			setupIndexHandlers(false);
			break;
		default:
			break;
		}
		if (settings.startServer()) {
			for (MeshTestInstance instance : instances) {
				instance.setupRestEndpoints(settings);
			}
		}
	}

	public void setupOnce(MeshTestSetting settings) throws Exception {
		if (settings == null) {
			throw new RuntimeException("Settings could not be found. Did you forget to add the @MeshTestSetting annotation to your test?");
		}

		int numberOfInstances = settings.clusterMode() ? settings.clusterInstances() : 1;

		meshTestContextProvider = MeshTestContextProvider.getProvider();
		MeshInstanceProvider<? extends MeshOptions> meshInstanceProvider = meshTestContextProvider.getInstanceProvider();

		meshInstanceProvider.getOptions();

		if (settings.synchronizeWrites()) {
			meshInstanceProvider.setSyncWrites(true);
		}

		meshInstanceProvider.initPhysicalStorage(settings);

		for (int i = 0; i < numberOfInstances; i++) {
			MeshOptions clonedMeshOptions = meshInstanceProvider.getClone();

			MeshTestInstance instance = new MeshTestInstance();
			instance.setupOnce(settings, clonedMeshOptions, i);
			instances.add(instance);
		}
	}

	private static OkHttpClient createTestClient() {

		try {
			// Create a trust manager that does not validate certificate chains
			final TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
				@Override
					public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
					public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new java.security.cert.X509Certificate[] {};
				}
				}
			};

			// Install the all-trusting trust manager
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			OkHttpClient.Builder builder = new OkHttpClient.Builder();
			int timeout = MeshAssert.getTimeout();
			builder.callTimeout(Duration.ofMinutes(timeout));
			builder.connectTimeout(Duration.ofMinutes(timeout));
			builder.writeTimeout(Duration.ofMinutes(timeout));
			builder.readTimeout(Duration.ofMinutes(timeout));
			builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
			builder.hostnameVerifier((hostName, sslSession) -> true);
			return builder.build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Test finished, so tear down the test context
	 * @param description
	 */
	protected void finished(Description description) {
		try {
			MeshTestSetting settings = getSettings(description);
			if (description.isSuite()) {
				tearDownOnce(settings);
			} else {
				tearDown(settings);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void tearDown(MeshTestSetting settings) throws Exception {
		tearDown(settings, false);
	}

	public void tearDown(MeshTestSetting settings, boolean force) throws Exception {
		if (!force) {
			switch (settings.resetBetweenTests()) {
			case NEVER:
				// the test does not require the database to be reset between test runs
				needsSetup = false;
				return;
			case ON_HASH_CHANGE:
				// we need to reset on a changed hash, which will be setup on test creation,
				// so we do not remove the data right now
				needsSetup = false;
				return;
			case ALWAYS:
			default:
				break;
			}
		}

		cleanupFolders();
		if (settings.startServer()) {
			for (MeshTestInstance instance : instances) {
				instance.undeployAndReset();
				instance.closeClient();
			}
		}
		if (idleConsumer != null) {
			idleConsumer.unregister();
			idleConsumer = null;
		}
		switch (settings.elasticsearch()) {
		case CONTAINER_ES7:
		case CONTAINER_ES8:
		case CONTAINER_ES6:
		case CONTAINER_ES6_TOXIC:
			instances.forEach(inst -> inst.meshDagger.searchProvider().clear().blockingAwait());
			break;
		case TRACKING:
			instances.forEach(inst -> inst.meshDagger.trackingSearchProvider().reset());
			break;
		default:
			break;
		}
		resetDatabase(settings);
	}

	public void tearDownOnce(MeshTestSetting settings) throws Exception {
		if (idleConsumer != null) {
			idleConsumer.unregister();
			idleConsumer = null;
		}
		for (MeshTestInstance instance : instances) {
			if (instance.mesh != null) {
				instance.mesh.shutdown();
			}
			if (instance.meshDagger != null) {
				instance.meshDagger.eventbusLivenessManager().shutdown();
			}
		}
		instances.clear();
		dataProvider = null;
		removeConfigDirectory();
		if (elasticsearch != null) {
			elasticsearch.stop();
		}
		if (keycloak != null) {
			keycloak.stop();
		}
		if (toxiproxy != null) {
			toxiproxy.stop();
			network.close();
		}
		meshTestContextProvider.getInstanceProvider().teardownStorage();
	}

	private void removeConfigDirectory() throws IOException {
		FileUtils.deleteDirectory(new File(CONF_PATH));
		System.setProperty("mesh.confDirName", CONF_PATH);
	}

	protected void setupIndexHandlers(boolean dataIndependent) throws Exception {
		for (MeshTestInstance instance : instances) {
			// We need to call init() again in order create missing indices for the created test data
			for (IndexHandler<?> handler : instance.meshDagger.indexHandlerRegistry().getHandlers()) {
				if (dataIndependent ^ handler.isDefinitionDataDependent()) {
					handler.init().blockingAwait();
				}
			}
		}
	}

	/**
	 * Set Features according to the method annotations
	 *
	 * @param description
	 */
	protected MeshTestSetting getSettings(Description description) {
		Class<?> testClass = description.getTestClass();
		if (testClass != null) {
			return testClass.getAnnotation(MeshTestSetting.class);
		}
		return description.getAnnotation(MeshTestSetting.class);
	}

	public int getHttpPort() {
		return instances.get(0).httpPort;
	}

	public int getHttpsPort() {
		return instances.get(0).httpsPort;
	}

	public Vertx getVertx() {
		return instances.get(0).vertx;
	}

	/**
	 * Setup the test data.
	 * 
	 * @param meshOptions
	 * @param setAdminPassword
	 *
	 * @throws Exception
	 */
	private void setupData(MeshOptions meshOptions, boolean setAdminPassword) throws Exception {
		instances.get(0).meshDagger.database().setMassInsertIntent();
		dataProvider.setup(meshOptions, setAdminPassword);
		instances.get(0).meshDagger.database().resetIntent();
	}

	/**
	 * Clear the test data.
	 *
	 * @param settings
	 * @throws Exception
	 */
	private void resetDatabase(MeshTestSetting settings) throws Exception {
		for (MeshTestInstance instance : instances) {
			instance.stopJobWorker();
		}

		instances.forEach(inst -> inst.meshDagger.boot().clearReferences());
		long start = System.currentTimeMillis();
		if (settings.inMemoryDB() || settings.clusterMode()) {
			if (!meshTestContextProvider.getInstanceProvider().fastStorageCleanup(
					instances.stream().map(inst -> inst.meshDagger.database()).collect(Collectors.toList()))) {
				instances.get(0).meshDagger.database().clear();
			}
		} else {
			instances.forEach(inst -> inst.meshDagger.database().stop());
			meshTestContextProvider.getInstanceProvider().cleanupPhysicalStorage();
			for (MeshTestInstance instance : instances) {
				instance.meshDagger.database().setupConnectionPool();
			}
		}
		for (MeshTestInstance instance : instances) {
			instance.meshDagger.jobWorkerVerticle().start();
		}
		long duration = System.currentTimeMillis() - start;
		LOG.info("Clearing DB took {" + duration + "} ms.");
		for (MeshTestInstance instance : instances) {
			if (instance.trackingSearchProvider != null) {
				instance.trackingSearchProvider.reset();
			}
		}
	}

	private void cleanupFolders() throws IOException {
		Predicate<Throwable> isFileNotFound = e -> 
				(e instanceof NoSuchFileException) 
				|| (e.getCause() instanceof NoSuchFileException)
				|| (e instanceof FileNotFoundException) 
				|| (e.getCause() instanceof FileNotFoundException);

		for (File folder : tmpFolders) {
			try {
				FileUtils.deleteDirectory(folder);
			} catch (IOException e) {
				if (isFileNotFound.test(e)) {
					LOG.debug("Suppressing inexisting directory deletion error", e);
				} else if (e instanceof IOExceptionList) {
					IOExceptionList el = IOExceptionList.class.cast(e);
					if (el.getCauseList().stream().allMatch(isFileNotFound)) {
						LOG.debug("Suppressing inexisting directory deletion errors", el);
					} else {
						throw el;
					}
				} else {
					throw e;
				}
			}
		}
		for (MeshTestInstance instance : instances) {
			if (instance.meshDagger != null && instance.meshDagger.permissionCache() != null) {
				instance.meshDagger.permissionCache().clear(false);
			}
		}
	}

	public TestDataProvider getData() {
		return dataProvider;
	}

	public TrackingSearchProvider getTrackingSearchProvider() {
		return instances.get(0).trackingSearchProvider;
	}

	private void initFolders(MeshOptions meshOptions) throws Exception {
		String tmpDir = newFolder("tmpDir");
		meshOptions.setTempDirectory(tmpDir);

		String uploads = newFolder("testuploads");
		meshOptions.getUploadOptions().setDirectory(uploads);

		String targetUploadTmpDir = newFolder("uploadTmpDir");
		meshOptions.getUploadOptions().setTempDirectory(targetUploadTmpDir);

		String imageCacheDir = newFolder("image_cache");
		meshOptions.getImageOptions().setImageCacheDirectory(imageCacheDir);

		String plugindirPath = newFolder("plugins");
		meshOptions.setPluginDirectory(plugindirPath);

		meshTestContextProvider.getInstanceProvider().initFolders(this::newFolder);
	}

	/**
	 * Create a new folder which will be automatically be deleted once the rule finishes.
	 *
	 * @param prefix
	 * @return
	 * @throws IOException
	 */
	private String newFolder(String prefix) throws IOException {
		String path = "target/" + prefix + "_" + UUIDUtil.randomUUID();
		File directory = new File(path);
		FileUtils.deleteDirectory(directory);
		directory.deleteOnExit();
		assertTrue("Could not create dir for path {" + path + "}", directory.mkdirs());
		tmpFolders.add(directory);
		return path;
	}

	@NotNull
	protected MeshComponent.Builder getMeshDaggerBuilder() {
		return meshTestContextProvider.getInstanceProvider().getComponentBuilder();
	}

	public MeshComponent createMeshComponent(MeshOptions options, MeshTestSetting settings, Mesh mesh) {
		return getMeshDaggerBuilder()
			.configuration(options)
			.searchProviderType(settings.elasticsearch().toSearchProviderType())
			.mesh(mesh)
			.build();
	}

	public MonitoringRestClient getMonitoringClient() {
		return instances.get(0).monitoringClient;
	}

	public MeshRestClient getHttpClient() {
		return getHttpClient(0);
	}

	public MeshRestClient getHttpClient(int instance) {
		return instances.get(instance).getHttpClient();
	}

	public MeshRestClient getHttpsClient() {
		return instances.get(0).getHttpsClient();
	}

	public MeshRestClient getHttpClient(String version) {
		return instances.get(0).getHttpClient(version);
	}

	public static KeycloakContainer getKeycloak() {
		return keycloak;
	}

	private void listenToSearchIdleEvent() {
		idleConsumer = instances.get(0).vertx.eventBus().consumer(MeshEvent.SEARCH_IDLE.address, handler -> {
			LOG.info("Got search idle event");
			if (idleLatch != null) {
				idleLatch.countDown();
			}
		});
	}

	public void waitAndClearSearchIdleEvents() {
		if (instances.isEmpty() || null == instances.get(0).mesh || null == instances.get(0).mesh.getOptions().getSearchOptions().getUrl()
				|| UNREACHABLE_HOST.equals(instances.get(0).mesh.getOptions().getSearchOptions().getUrl())) {
			return;
		}
		waitForSearchIdleEvent();
		if (instances.get(0).trackingSearchProvider != null) {
			instances.get(0).trackingSearchProvider.clear().blockingAwait();
		}
	}

	/**
	 * Waits until all requests have been sent successfully.
	 */
	public void waitForSearchIdleEvent() {
		int MAX_WAIT_TIME = 30;
		Objects.requireNonNull(idleConsumer, "Call #listenToSearchIdleEvent first");
		ElasticsearchProcessVerticle verticle = getElasticSearchVerticle();
		try {
			idleLatch = new CountDownLatch(1);
			verticle.flush().blockingAwait();
			boolean success = idleLatch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
			if (!success) {
				throw new RuntimeException("Timed out after " + MAX_WAIT_TIME + " seconds waiting for search idle event.");
			}
			verticle.refresh().blockingAwait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public ElasticsearchProcessVerticle getElasticSearchVerticle() {
		return ((AbstractBootstrapInitializer) instances.get(0).meshDagger.boot()).getCoreVerticleLoader().getSearchVerticle();
	}

	public static ContainerProxy getProxy() {
		return proxy;
	}

	public static ElasticsearchContainer elasticsearchContainer() {
		return elasticsearch;
	}

	public MeshComponent getMeshComponent() {
		return instances.get(0).meshDagger;
	}

	public Mesh getMesh() {
		return instances.get(0).mesh;
	}

	public MeshInstanceProvider<? extends MeshOptions> getInstanceProvider() {
		return meshTestContextProvider.getInstanceProvider();
	}

	public Comparator<String> getSortComparator() {
		return meshTestContextProvider.sortComparator();
	}

	public MeshOptions getOptions() {
		return getOptions(0);
	}

	public MeshOptions getOptions(int instance) {
		return instances.get(instance).getOptions();
	}

	public MeshTestActions actions() {
		return getInstanceProvider().actions();
	}

	/**
	 * Does the test need setup? This will be true whenever the database has been setup from scratch
	 * @return true if the test needs to be setup
	 */
	public boolean needsSetup() {
		return needsSetup;
	}

	/**
	 * Set the db hash for the current test run, which is used to determine, whether
	 * the db needs to be reset (when {@link MeshTestSetting#resetBetweenTests()} is
	 * set to {@link ResetTestDb#ON_HASH_CHANGE}).
	 * This method must be called in the constructor of the test.
	 * 
	 * @param hash db hash for the current test
	 */
	public void setDbHash(int hash) {
		this.currentDbHash = hash;
	}

	public class MeshTestInstance {
		private Mesh mesh;

		private MeshComponent meshDagger;

		private Vertx vertx;

		protected int httpPort;
		protected int httpsPort;
		protected int monitoringPort;

		// Maps api version to client
		private final Map<String, MeshRestClient> clients = new HashMap<>();

		private MonitoringRestClient monitoringClient;

		private TrackingSearchProvider trackingSearchProvider;

		/**
		 * Initialise the mesh dagger context and inject the dependencies within the test.
		 *
		 * @param options
		 *
		 * @param settings
		 * @throws Exception
		 */
		public void initDagger(MeshOptions options, MeshTestSetting settings) throws Exception {
			LOG.info("Initializing dagger context");
			try {
				@NotNull MeshComponent.Builder builder = getMeshDaggerBuilder();
				mesh = new MeshImpl(options, builder);
				meshDagger = createMeshComponent(options, settings);
				if (dataProvider == null) {
					dataProvider = new TestDataProvider(settings.testSize(), meshDagger.boot(), meshDagger.database(), meshDagger.batchProvider());
				}
				if (meshDagger.searchProvider() instanceof TrackingSearchProviderImpl) {
					trackingSearchProvider = meshDagger.trackingSearchProvider();
				}
				mesh.setMeshInternal(meshDagger);
				// We omit creating the initial admin password since hashing the password would slow down tests
				if (!getOptions().getInitialAdminPassword().startsWith("debug")) {
					getOptions().setInitialAdminPassword(null);
				}
				meshDagger.boot().init(mesh, false, options, null);
				vertx = meshDagger.boot().vertx();
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		}

		public void setupOnce(MeshTestSetting settings, MeshOptions meshOptions, int instanceNumber) throws Exception {
			httpPort = TestUtils.getRandomPort();
			httpsPort = TestUtils.getRandomPort();
			monitoringPort = TestUtils.getRandomPort();
			removeConfigDirectory();
			MeshOptions options = init(settings, meshOptions, instanceNumber);
			try {
				initDagger(options, settings);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Error while creating dagger dependency graph", e);
			}
			meshDagger.boot().registerEventHandlers();

		}

		/**
		 * Initialize mesh options.
		 *
		 * @param settings
		 * @param meshOptions
		 * @param instanceNumber
		 * @throws Exception
		 */
		public MeshOptions init(MeshTestSetting settings, MeshOptions meshOptions, int instanceNumber) throws Exception {
			if (settings == null) {
				throw new RuntimeException("Settings could not be found. Did you forget to add the @MeshTestSetting annotation to your test?");
			}

			// restrict number of verticles and threads
			meshOptions.getHttpServerOptions().setVerticleAmount(10);
			meshOptions.getVertxOptions().setWorkerPoolSize(5);
			meshOptions.getVertxOptions().setEventPoolSize(10);

			// disable usage of "ordered" blocking handlers (which seems to be useless anyways)
			meshOptions.getVertxOptions().setOrderedBlockingHandlers(false);

			// disable periodic index check
			meshOptions.getSearchOptions().setIndexCheckInterval(0);

			// Clustering options
			if (settings.clusterMode()) {
				meshOptions.getClusterOptions().setEnabled(true);
				meshOptions.setInitCluster(true);
				meshOptions.getClusterOptions()
						.setClusterName(StringUtils.isBlank(settings.clusterName()) ? "cluster" + System.currentTimeMillis()
								: settings.clusterName());
				meshOptions.getClusterOptions().setVertxPort(TestUtils.getRandomPort());
			}

			// Monitoring
			meshOptions.getMonitoringOptions().setEnabled(settings.monitoring());

			// Setup the keystore
			File keystoreFile = new File("target", "keystore_" + UUIDUtil.randomUUID() + ".jceks");
			keystoreFile.deleteOnExit();
			String keystorePassword = "finger";
			if (!keystoreFile.exists()) {
				KeyStoreHelper.gen(keystoreFile.getAbsolutePath(), keystorePassword);
			}
			AuthenticationOptions authOptions = meshOptions.getAuthenticationOptions();
			authOptions.setKeystorePassword(keystorePassword);
			authOptions.setKeystorePath(keystoreFile.getAbsolutePath());

			String nodeName = "testNode";
			if (settings.clusterMode()) {
				List<String> nodeNames = Arrays.asList(settings.nodeNames());
				if (instanceNumber >= nodeNames.size()) {
					nodeName = "testNode-" + clusterNodeCounter.incrementAndGet();
				} else {
					nodeName = nodeNames.get(instanceNumber);
				}
			}
			meshOptions.setNodeName(nodeName);

			initFolders(meshOptions);

			HttpServerConfig httpOptions = meshOptions.getHttpServerOptions();
			httpOptions.setPort(httpPort);
			switch (settings.ssl()) {
			case OFF:
				httpOptions.setSsl(false);
				break;

			case NORMAL:
				File certPem = MeshTestHelper.extractResource("/ssl/cert.pem");
				File keyPem = MeshTestHelper.extractResource("/ssl/key.pem");

				httpOptions.setSsl(true);
				httpOptions.setSslPort(httpsPort);
				httpOptions.setCertPath(certPem.getAbsolutePath());
				httpOptions.setKeyPath(keyPem.getAbsolutePath());
				break;

			case CLIENT_CERT_REQUEST:
				File serverPem = MeshTestHelper.extractResource("/client-ssl/server.pem");
				File serverKey = MeshTestHelper.extractResource("/client-ssl/server.key");

				httpOptions.setClientAuthMode(ClientAuth.REQUEST);
				httpOptions.setSsl(true);
				httpOptions.setSslPort(httpsPort);
				httpOptions.setCertPath(serverPem.getAbsolutePath());
				httpOptions.setKeyPath(serverKey.getAbsolutePath());
				httpOptions.setTrustedCertPaths(Arrays.asList(serverPem.getAbsolutePath()));
				break;

			case CLIENT_CERT_REQUIRED:
				serverPem = MeshTestHelper.extractResource("/client-ssl/server.pem");
				serverKey = MeshTestHelper.extractResource("/client-ssl/server.key");

				httpOptions.setClientAuthMode(ClientAuth.REQUIRED);
				httpOptions.setSsl(true);
				httpOptions.setSslPort(httpsPort);
				httpOptions.setCertPath(serverPem.getAbsolutePath());
				httpOptions.setKeyPath(serverKey.getAbsolutePath());
				httpOptions.setTrustedCertPaths(Arrays.asList(serverPem.getAbsolutePath()));
				break;
			}

			MonitoringConfig monitoringOptions = meshOptions.getMonitoringOptions();
			monitoringOptions.setPort(monitoringPort);

			ElasticSearchOptions searchOptions = meshOptions.getSearchOptions();
			S3Options s3Options = meshOptions.getS3Options();
			searchOptions.setTimeout(10_000L);

			String version = switch (settings.elasticsearch()) {
				case CONTAINER_ES7 -> {
					searchOptions.setComplianceMode(ComplianceMode.ES_7);

					yield ElasticsearchContainer.VERSION_ES7;
				}

				case CONTAINER_ES8 -> {
					searchOptions.setComplianceMode(ComplianceMode.ES_8);

					yield ElasticsearchContainer.VERSION_ES8;
				}

				default -> ElasticsearchContainer.VERSION_ES6;
			};

			switch (settings.elasticsearch()) {
			case CONTAINER_ES6:
			case CONTAINER_ES7:
			case CONTAINER_ES8:
			case UNREACHABLE:
				elasticsearch = new ElasticsearchContainer(version);
				if (!elasticsearch.isRunning()) {
					elasticsearch.start();
				}
				elasticsearch.waitingFor(Wait.forHttp("/"));

				if (settings.elasticsearch() == UNREACHABLE) {
					searchOptions.setUrl(UNREACHABLE_HOST);
				} else {
					searchOptions.setUrl("http://" + elasticsearch.getHost() + ":" + elasticsearch.getMappedPort(9200));
				}
				if (settings.elasticsearch() == ElasticsearchTestMode.CONTAINER_ES8) {
					Thread.sleep(1000);
					SearchProviderModule.searchClient(meshOptions).settings(new JsonObject("{\n"
							+ "    \"persistent\": {\n"
							+ "        \"action.destructive_requires_name\": false\n"
							+ "    }\n"
							+ "}"))
					.sync();
				}
				break;
			case CONTAINER_ES6_TOXIC:
				network = Network.newNetwork();
				elasticsearch = new ElasticsearchContainer(version).withNetwork(network);
				elasticsearch.waitingFor(Wait.forHttp(("/")));
				toxiproxy = new ToxiproxyContainer(DockerImageName
						.parse(System.getProperty("mesh.container.image.prefix", "") + "shopify/toxiproxy:2.1.0")
						.asCompatibleSubstituteFor("shopify/toxiproxy:2.1.0")).withNetwork(network);
				if (!toxiproxy.isRunning()) {
					toxiproxy.start();
				}
				proxy = toxiproxy.getProxy(elasticsearch, 9200);

				final String ipAddressViaToxiproxy = proxy.getContainerIpAddress();
				final int portViaToxiproxy = proxy.getProxyPort();

				if (!elasticsearch.isRunning()) {
					elasticsearch.start();
				}
				searchOptions.setUrl("http://" + ipAddressViaToxiproxy + ":" + portViaToxiproxy);
				break;
			case NONE:
				searchOptions.setUrl(null);
				break;
			case TRACKING:
				System.setProperty(TrackingSearchProviderImpl.TEST_PROPERTY_KEY, "true");
				break;
			default:
				break;
			}
			switch (settings.awsContainer()) {
			case NONE:
				break;
			case AWS:
				throw new IllegalStateException("AWS test container is currently unsupported");
			case MINIO:
				String ACCESS_KEY = "accessKey";
				String SECRET_KEY = "secretKey";
					AWSContainer awsContainer = new AWSContainer(
							new AWSContainer.CredentialsProvider(ACCESS_KEY, SECRET_KEY));
				awsContainer.start();
				s3Options.setCorsAllowedOrigins(null);
				s3Options.setCorsAllowedHeaders(null);
				s3Options.setCorsAllowedMethods(null);
				s3Options.setEnabled(true);
				s3Options.setAccessKeyId(ACCESS_KEY);
				s3Options.setBucket("test-bucket");
				S3CacheOptions s3CacheOptions = new S3CacheOptions();
				s3CacheOptions.setBucket("test-cache-bucket");
				s3Options.setS3CacheOptions(s3CacheOptions);
				s3Options.setSecretAccessKey(SECRET_KEY);
				s3Options.setRegion("eu-central-1");
				s3Options.setEndpoint("http://" + awsContainer.getHostAddress());
				break;
			}

			if (settings.useKeycloak()) {
				keycloak = new KeycloakContainer("/keycloak/realm.json", "keycloak/keycloak", "22.0.5", Arrays.asList("start-dev"), true)
						.waitingFor(Wait.forHttp("/realms/master-test"));

				if (!keycloak.isRunning()) {
					keycloak.start();
				}
				String realmName = "master-test";
				Set<JsonObject> jwks = KeycloakUtils.loadJWKs("http", keycloak.getHost(), keycloak.getMappedPort(8080), realmName);
				meshOptions.getAuthenticationOptions().setPublicKeys(jwks);
			}
			settings.optionChanger().change(meshOptions);
			settings.customOptionChanger().getConstructor().newInstance().change(meshOptions);
			return meshOptions;
		}

		public MeshComponent createMeshComponent(MeshOptions options, MeshTestSetting settings) {
			return meshDagger = MeshTestContext.this.createMeshComponent(options, settings, mesh);
		}

		public MeshRestClient getHttpClient() {
			return clients.get("http_v" + CURRENT_API_VERSION);
		}

		public MeshRestClient getHttpsClient() {
			return clients.get("https_v" + CURRENT_API_VERSION);
		}

		public MeshRestClient getHttpClient(String version) {
			return clients.get("http_" + version);
		}

		public MeshOptions getOptions() {
			return mesh.getOptions();
		}

		public Mesh getMesh() {
			return mesh;
		}

		private void setupRestEndpoints(MeshTestSetting settings) throws Exception {
			mesh.getOptions().getUploadOptions().setByteLimit(Long.MAX_VALUE);

			LOG.info("Using port:  " + httpPort);
			meshDagger.routerStorageRegistry().addProject(TestDataProvider.PROJECT_NAME);

			// Setup the rest client
			db().tx(tx -> {
				MeshRestClientConfig.Builder httpConfigBuilder = new MeshRestClientConfig.Builder()
					.setHost("localhost")
					.setPort(httpPort)
					.setBasePath(CURRENT_API_BASE_PATH)
					.setSsl(false);

				MeshRestClient httpClient = MeshRestClient.create(httpConfigBuilder.build(), okHttp);
				httpClient.setLogin(getData().user().getUsername(), getData().getUserInfo().getPassword());
				httpClient.login().blockingGet();
				clients.put("http_v" + CURRENT_API_VERSION, httpClient);

				// Setup SSL client if needed
				SSLTestMode ssl = settings.ssl();
				MeshRestClientConfig.Builder httpsConfigBuilder = new MeshRestClientConfig.Builder()
					.setHost("localhost")
					.setPort(httpsPort)
					.setBasePath(CURRENT_API_BASE_PATH)
					.setHostnameVerification(false)
					.setSsl(true);

				MeshRestClientConfig httpsConfig = null;
				switch (ssl) {
				case OFF:
					break;

				case CLIENT_CERT_REQUEST:
				case CLIENT_CERT_REQUIRED:
					File serverPem = MeshTestHelper.extractResource("/client-ssl/server.pem");
					File alicePem = MeshTestHelper.extractResource("/client-ssl/alice.pem");
					File aliceKey = MeshTestHelper.extractResource("/client-ssl/alice.key");

					httpsConfigBuilder.addTrustedCA(serverPem.getAbsolutePath());
					httpsConfigBuilder.setClientCert(alicePem.getAbsolutePath());
					httpsConfigBuilder.setClientKey(aliceKey.getAbsolutePath());
					httpsConfig = httpsConfigBuilder.build();
					break;

				case NORMAL:
					httpsConfig = httpsConfigBuilder.build();
					break;
				}

				if (httpsConfig != null) {
					MeshRestClient httpsClient = MeshRestClient.create(httpsConfig);
					httpsClient.setLogin(getData().user().getUsername(), getData().getUserInfo().getPassword());
					httpsClient.login().blockingGet();
					clients.put("https_v" + CURRENT_API_VERSION, httpsClient);
				}

				IntStream.range(1, CURRENT_API_VERSION).forEach(version -> {
					MeshRestClient oldClient = MeshRestClient.create(httpConfigBuilder.setBasePath("/api/v" + version).build());
					oldClient.setAuthenticationProvider(httpClient.getAuthentication());
					clients.put("http_v" + version, oldClient);
				});
			});
			LOG.info("Using monitoring port: " + monitoringPort);
			MonitoringClientConfig monitoringClientConfig = new MonitoringClientConfig.Builder()
				.setBasePath(CURRENT_API_BASE_PATH)
				.setHost("localhost")
				.setPort(monitoringPort)
				.build();

			monitoringClient = MonitoringRestClient.create(monitoringClientConfig);
			if (trackingSearchProvider != null) {
				trackingSearchProvider.clear().blockingAwait();
			}
		}

		private Database db() {
			return meshDagger.database();
		}

		private void undeployAndReset() throws Exception {
			for (String id : deploymentIds) {
				vertx.undeploy(id);
			}
		}

		private void closeClient() throws Exception {
			clients.values().forEach(client -> {
				if (client != null) {
					try {
						client.close();
					} catch (IllegalStateException e) {
						// Ignored
						e.printStackTrace();
					}
				}
			});
		}

		/**
		 * Stop the JobWorkerVerticle and wait for any jobs, which are still running
		 * @throws Exception
		 */
		private void stopJobWorker() throws Exception {
			long start = System.currentTimeMillis();

			JobWorkerVerticle jobWorkerVerticle = meshDagger.jobWorkerVerticle();
			jobWorkerVerticle.stop();

			// if any jobs are currently running, they will hold the global lock.
			// so we will wait until we can acquire the lock, which makes sure that
			// currently running jobs will have stopped.
			CountDownLatch jobWait = new CountDownLatch(1);
			jobWorkerVerticle.doWithLock(1000, rh -> {
				if (rh.succeeded()) {
					rh.result().release();
				} else {
					LOG.warn("Failed to get job worker verticle lock in");
				}
				jobWait.countDown();
			});
			jobWait.await(2, TimeUnit.SECONDS);
			LOG.info(String.format("Stopping JobWorkerVerticle took {%d} ms", System.currentTimeMillis() - start));
		}
	}
}
