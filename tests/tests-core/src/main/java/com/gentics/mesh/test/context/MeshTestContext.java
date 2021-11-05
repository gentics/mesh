package com.gentics.mesh.test.context;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.MeshVersion.CURRENT_API_VERSION;
import static com.gentics.mesh.test.ElasticsearchTestMode.UNREACHABLE;
import static com.gentics.mesh.test.context.MeshTestHelper.noopConsumer;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.ToxiproxyContainer.ContainerProxy;
import org.testcontainers.containers.wait.strategy.Wait;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.util.KeycloakUtils;
import com.gentics.mesh.cli.AbstractBootstrapInitializer;
import com.gentics.mesh.cli.MeshImpl;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.crypto.KeyStoreHelper;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;
import com.gentics.mesh.etc.config.S3CacheOptions;
import com.gentics.mesh.etc.config.S3Options;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.monitoring.MonitoringClientConfig;
import com.gentics.mesh.rest.monitoring.MonitoringRestClient;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.TrackingSearchProviderImpl;
import com.gentics.mesh.search.verticle.ElasticsearchProcessVerticle;
import com.gentics.mesh.test.MeshInstanceProvider;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestContextProvider;
import com.gentics.mesh.test.MeshTestSetting;
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
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.OkHttpClient;

public class MeshTestContext extends TestWatcher {

	static {
		System.setProperty(TrackingSearchProviderImpl.TEST_PROPERTY_KEY, "true");
	}

	private static final Logger log = LoggerFactory.getLogger(MeshTestContext.class);

	private static final String CONF_PATH = "target/config-" + System.currentTimeMillis();

	public static ElasticsearchContainer elasticsearch;

	public static KeycloakContainer keycloak;

	public static Network network;

	public static ToxiproxyContainer toxiproxy;

	public static ContainerProxy proxy;

	public static OkHttpClient okHttp = createTestClient();

	private List<File> tmpFolders = new ArrayList<>();
	private MeshComponent meshDagger;
	private TestDataProvider dataProvider;
	private TrackingSearchProvider trackingSearchProvider;
	private Vertx vertx;

	protected int httpPort;
	protected int httpsPort;
	protected int monitoringPort;

	// Maps api version to client
	private final Map<String, MeshRestClient> clients = new HashMap<>();

	private MonitoringRestClient monitoringClient;

	private List<String> deploymentIds = new ArrayList<>();

	private CountDownLatch idleLatch;
	private MessageConsumer<Object> idleConsumer;

	private Consumer<MeshOptions> optionChanger = noopConsumer();

	private Mesh mesh;

	private MeshTestContextProvider meshTestContextProvider;

	@Override
	protected void starting(Description description) {
		try {
			MeshTestSetting settings = getSettings(description);
			// Setup the dagger context and orientdb,es once
			if (description.isSuite()) {
				setupOnce(settings);
			} else {
				setup(settings);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void setup(MeshTestSetting settings) throws Exception {
		if (!settings.inMemoryDB() && (meshDagger.database() instanceof GraphDatabase)) {
			DatabaseHelper.init(HibClassConverter.toGraph(meshDagger.database()));
		}
		initFolders(mesh.getOptions());
		boolean setAdminPassword = settings.optionChanger() != MeshOptionChanger.INITIAL_ADMIN_PASSWORD;
		setupData(mesh.getOptions(), setAdminPassword);
		listenToSearchIdleEvent();
		switch (settings.elasticsearch()) {
		case CONTAINER_ES6:
		case CONTAINER_ES7:
			setupIndexHandlers();
			break;
		default:
			break;
		}
		if (settings.startServer()) {
			setupRestEndpoints(settings);
		}
	}

	public void setupOnce(MeshTestSetting settings) throws Exception {
		httpPort = TestUtils.getRandomPort();
		httpsPort = TestUtils.getRandomPort();
		monitoringPort = TestUtils.getRandomPort();
		removeConfigDirectory();
		MeshOptions options = init(settings);
		try {
			initDagger(options, settings);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error while creating dagger dependency graph", e);
		}
		meshDagger.boot().registerEventHandlers();

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

	@Override
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
		cleanupFolders();
		if (settings.startServer()) {
			undeployAndReset();
			closeClient();
		}
		idleConsumer.unregister();
		switch (settings.elasticsearch()) {
		case CONTAINER_ES7:
		case CONTAINER_ES6:
		case CONTAINER_ES6_TOXIC:
		case EMBEDDED:
			meshDagger.searchProvider().clear().blockingAwait();
			break;
		case TRACKING:
			meshDagger.trackingSearchProvider().reset();
			break;
		default:
			break;
		}
		resetDatabase(settings);

	}

	public void tearDownOnce(MeshTestSetting settings) throws Exception {
		// TODO CI does not like this, reactivate later:
		// mesh.shutdown();
		removeConfigDirectory();
		if (elasticsearch != null && elasticsearch.isRunning()) {
			elasticsearch.stop();
		}
		if (keycloak != null && keycloak.isRunning()) {
			keycloak.stop();
		}
		if (toxiproxy != null) {
			toxiproxy.stop();
			network.close();
		}
		optionChanger = noopConsumer();

	}

	private void removeConfigDirectory() throws IOException {
		FileUtils.deleteDirectory(new File(CONF_PATH));
		System.setProperty("mesh.confDirName", CONF_PATH);
	}

	protected void setupIndexHandlers() throws Exception {
		// We need to call init() again in order create missing indices for the created test data
		for (IndexHandler<?> handler : meshDagger.indexHandlerRegistry().getHandlers()) {
			handler.init().blockingAwait();
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

	private void setupRestEndpoints(MeshTestSetting settings) throws Exception {
		mesh.getOptions().getUploadOptions().setByteLimit(Long.MAX_VALUE);

		log.info("Using port:  " + httpPort);
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
		log.info("Using monitoring port: " + monitoringPort);
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

	public int getHttpPort() {
		return httpPort;
	}

	public int getHttpsPort() {
		return httpsPort;
	}

	public Vertx getVertx() {
		return vertx;
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
		meshDagger.database().setMassInsertIntent();
		dataProvider.setup(meshOptions, setAdminPassword);
		meshDagger.database().resetIntent();
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
	 * Clear the test data.
	 *
	 * @param settings
	 * @throws Exception
	 */
	private void resetDatabase(MeshTestSetting settings) throws Exception {
		meshDagger.boot().clearReferences();
		long start = System.currentTimeMillis();
		if (settings.inMemoryDB()) {
			meshDagger.database().clear();
		} else if (settings.clusterMode()) {
			meshDagger.database().clear();
		} else {
			meshDagger.database().stop();

			meshTestContextProvider.getInstanceProvider().cleanupPhysicalStorage();

			meshDagger.database().setupConnectionPool();
		}
		long duration = System.currentTimeMillis() - start;
		log.info("Clearing DB took {" + duration + "} ms.");
		if (trackingSearchProvider != null) {
			trackingSearchProvider.reset();
		}
	}

	private void cleanupFolders() throws IOException {
		for (File folder : tmpFolders) {
			FileUtils.deleteDirectory(folder);
		}
		meshDagger.permissionCache().clear(false);
	}

	public TestDataProvider getData() {
		return dataProvider;
	}

	public TrackingSearchProvider getTrackingSearchProvider() {
		return trackingSearchProvider;
	}

	/**
	 * Initialize mesh options.
	 *
	 * @param settings
	 * @throws Exception
	 */
	public MeshOptions init(MeshTestSetting settings) throws Exception {
		if (settings == null) {
			throw new RuntimeException("Settings could not be found. Did you forget to add the @MeshTestSetting annotation to your test?");
		}

		meshTestContextProvider = MeshTestContextProvider.getProvider();

		MeshInstanceProvider<? extends MeshOptions> meshInstanceProvider = meshTestContextProvider.getInstanceProvider();

		MeshOptions meshOptions = meshInstanceProvider.getOptions();


		// disable periodic index check
		meshOptions.getSearchOptions().setIndexCheckInterval(0);

		// Clustering options
		if (settings.clusterMode()) {
			meshOptions.getClusterOptions().setEnabled(true);
			meshOptions.setInitCluster(true);
			meshOptions.getClusterOptions().setClusterName("cluster" + System.currentTimeMillis());
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
		meshOptions.setNodeName("testNode");

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
			File serverKey = MeshTestHelper.extractResource("/server.key");

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

		meshInstanceProvider.initStorage(settings);

		ElasticSearchOptions searchOptions = meshOptions.getSearchOptions();
		S3Options s3Options = meshOptions.getS3Options();
		searchOptions.setTimeout(10_000L);

		String version = ElasticsearchContainer.VERSION_ES6;
		switch (settings.elasticsearch()) {
		case CONTAINER_ES7:
			searchOptions.setComplianceMode(ComplianceMode.ES_7);
			version = ElasticsearchContainer.VERSION_ES7;
		case CONTAINER_ES6:
		case UNREACHABLE:
			elasticsearch = new ElasticsearchContainer(version);
			if (!elasticsearch.isRunning()) {
				elasticsearch.start();
			}
			elasticsearch.waitingFor(Wait.forHttp("/"));

			searchOptions.setStartEmbedded(false);
			if (settings.elasticsearch() == UNREACHABLE) {
				searchOptions.setUrl("http://localhost:1");
			} else {
				searchOptions.setUrl("http://" + elasticsearch.getHost() + ":" + elasticsearch.getMappedPort(9200));
			}
			break;
		case CONTAINER_ES6_TOXIC:
			network = Network.newNetwork();
			elasticsearch = new ElasticsearchContainer(version).withNetwork(network);
			elasticsearch.waitingFor(Wait.forHttp(("/")));
			toxiproxy = new ToxiproxyContainer().withNetwork(network);
			if (!toxiproxy.isRunning()) {
				toxiproxy.start();
			}
			proxy = toxiproxy.getProxy(elasticsearch, 9200);

			final String ipAddressViaToxiproxy = proxy.getContainerIpAddress();
			final int portViaToxiproxy = proxy.getProxyPort();

			if (!elasticsearch.isRunning()) {
				elasticsearch.start();
			}
			searchOptions.setStartEmbedded(false);
			searchOptions.setUrl("http://" + ipAddressViaToxiproxy + ":" + portViaToxiproxy);
			break;
		case EMBEDDED:
			searchOptions.setComplianceMode(ComplianceMode.ES_6);
			searchOptions.setStartEmbedded(true);
			break;
		case NONE:
			searchOptions.setUrl(null);
			searchOptions.setStartEmbedded(false);
			break;
		case TRACKING:
			System.setProperty(TrackingSearchProviderImpl.TEST_PROPERTY_KEY, "true");
			searchOptions.setStartEmbedded(false);
			break;
		default:
			break;
		}
		switch (settings.awsContainer()) {
			case AWS:
				break;
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
			keycloak = new KeycloakContainer("/keycloak/realm.json").waitingFor(Wait.forHttp("/auth/realms/master-test"));
			if (!keycloak.isRunning()) {
				keycloak.start();
			}
			String realmName = "master-test";
			Set<JsonObject> jwks = KeycloakUtils.loadJWKs("http", keycloak.getHost(), keycloak.getMappedPort(8080), realmName);
			meshOptions.getAuthenticationOptions().setPublicKeys(jwks);
		}
		settings.optionChanger().changer.accept(meshOptions);
		optionChanger.accept(meshOptions);
		return meshOptions;
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

	/**
	 * Initialise the mesh dagger context and inject the dependencies within the test.
	 *
	 * @param options
	 *
	 * @param settings
	 * @throws Exception
	 */
	public void initDagger(MeshOptions options, MeshTestSetting settings) throws Exception {
		log.info("Initializing dagger context");
		try {
			@NotNull MeshComponent.Builder builder = getMeshDaggerBuilder();
			mesh = new MeshImpl(options, builder);
			meshDagger = this.createMeshComponent(mesh, options, settings);
			dataProvider = new TestDataProvider(settings.testSize(), meshDagger.boot(), meshDagger.database(), meshDagger.batchProvider());
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

	@NotNull
	private MeshComponent.Builder getMeshDaggerBuilder() {
		return meshTestContextProvider.getInstanceProvider().getComponentBuilder();
	}

	public MeshComponent createMeshComponent(Mesh mesh, MeshOptions options, MeshTestSetting settings) {
		return meshDagger = getMeshDaggerBuilder()
			.configuration(options)
			.searchProviderType(settings.elasticsearch().toSearchProviderType())
			.mesh(mesh)
			.build();
	}

	public MonitoringRestClient getMonitoringClient() {
		return monitoringClient;
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

	public static KeycloakContainer getKeycloak() {
		return keycloak;
	}

	private void listenToSearchIdleEvent() {
		idleConsumer = vertx.eventBus().consumer(MeshEvent.SEARCH_IDLE.address, handler -> {
			log.info("Got search idle event");
			if (idleLatch != null) {
				idleLatch.countDown();
			}
		});
	}

	/**
	 * Waits until all requests have been sent successfully.
	 */
	public void waitForSearchIdleEvent() {
		int MAX_WAIT_TIME = 25;
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
		return ((AbstractBootstrapInitializer) meshDagger.boot()).loader.get().getSearchVerticle();
	}

	public static ContainerProxy getProxy() {
		return proxy;
	}

	public static ElasticsearchContainer elasticsearchContainer() {
		return elasticsearch;
	}

	public MeshTestContext setOptionChanger(Consumer<MeshOptions> optionChanger) {
		this.optionChanger = optionChanger;
		return this;
	}

	public MeshComponent getMeshComponent() {
		return meshDagger;
	}

	public Mesh getMesh() {
		return mesh;
	}

	public MeshInstanceProvider<? extends MeshOptions> getInstanceProvider() {
		return meshTestContextProvider.getInstanceProvider();
	}

	public MeshOptions getOptions() {
		return meshTestContextProvider.getOptions();
	}
}
