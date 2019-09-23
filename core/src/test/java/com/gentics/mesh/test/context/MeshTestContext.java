package com.gentics.mesh.test.context;

import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_VERSION;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.UNREACHABLE;
import static com.gentics.mesh.test.context.MeshTestHelper.noopConsumer;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.ToxiproxyContainer.ContainerProxy;
import org.testcontainers.containers.wait.strategy.Wait;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.KeycloakUtils;
import com.gentics.mesh.cli.BootstrapInitializerImpl;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.crypto.KeyStoreHelper;
import com.gentics.mesh.dagger.DaggerMeshComponent;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.monitoring.MonitoringClientConfig;
import com.gentics.mesh.rest.monitoring.MonitoringRestClient;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.verticle.ElasticsearchProcessVerticle;
import com.gentics.mesh.test.TestDataProvider;
import com.gentics.mesh.test.docker.ElasticsearchContainer;
import com.gentics.mesh.test.docker.KeycloakContainer;
import com.gentics.mesh.test.util.MeshAssert;
import com.gentics.mesh.test.util.TestUtils;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.OkHttpClient;

public class MeshTestContext extends TestWatcher {

	static {
		System.setProperty(TrackingSearchProvider.TEST_PROPERTY_KEY, "true");
	}

	private static final Logger log = LoggerFactory.getLogger(MeshTestContext.class);

	private static final String CONF_PATH = "target/config-" + System.currentTimeMillis();

	private static MeshOptions meshOptions;

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

	protected int port;
	protected int monitoringPort;

	// Maps api version to client
	private final Map<String, MeshRestClient> clients = new HashMap<>();

	private MonitoringRestClient monitoringClient;

	private List<String> deploymentIds = new ArrayList<>();

	private CountDownLatch idleLatch;
	private MessageConsumer<Object> idleConsumer;

	private Consumer<MeshOptions> optionChanger = noopConsumer();

	private Mesh mesh;

	@Override
	protected void starting(Description description) {
		try {
			MeshTestSetting settings = getSettings(description);
			// Setup the dagger context and orientdb,es once
			if (description.isSuite()) {
				port = TestUtils.getRandomPort();
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
			} else {
				if (!settings.inMemoryDB()) {
					DatabaseHelper.init(meshDagger.database());
				}
				initFolders(mesh.getOptions());
				setupData();
				listenToSearchIdleEvent();
				switch (settings.elasticsearch()) {
				case CONTAINER:
					setupIndexHandlers();
					break;
				default:
					break;
				}
				if (settings.startServer()) {
					setupRestEndpoints(settings);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
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
			builder.hostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
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
			} else {
				cleanupFolders();
				if (settings.startServer()) {
					undeployAndReset();
					closeClient();
				}
				idleConsumer.unregister();
				switch (settings.elasticsearch()) {
				case CONTAINER:
				case CONTAINER_TOXIC:
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
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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

		log.info("Using port:  " + port);
		meshDagger.routerStorageRegistry().addProject(TestDataProvider.PROJECT_NAME);

		// Setup the rest client
		try (Tx tx = db().tx()) {
			boolean ssl = settings.ssl();
			MeshRestClientConfig.Builder config = new MeshRestClientConfig.Builder()
				.setHost("localhost")
				.setPort(port)
				.setBasePath(CURRENT_API_BASE_PATH)
				.setSsl(ssl);

			MeshRestClient defaultClient = MeshRestClient.create(config.build(), okHttp);
			defaultClient.setLogin(getData().user().getUsername(), getData().getUserInfo().getPassword());
			defaultClient.login().blockingGet();
			clients.put("v" + CURRENT_API_VERSION, defaultClient);
			IntStream.range(1, CURRENT_API_VERSION).forEach(version -> {
				MeshRestClient oldClient = MeshRestClient.create(config.setBasePath("/api/v" + version).build());
				oldClient.setAuthenticationProvider(defaultClient.getAuthentication());
				clients.put("v" + version, oldClient);
			});
		}
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

	public int getPort() {
		return port;
	}

	public Vertx getVertx() {
		return vertx;
	}

	/**
	 * Setup the test data.
	 *
	 * @throws Exception
	 */
	private void setupData() throws Exception {
		meshDagger.database().setMassInsertIntent();
		dataProvider.setup();
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
			String dir = mesh.getOptions().getStorageOptions().getDirectory();
			File dbDir = new File(dir);
			FileUtils.deleteDirectory(dbDir);
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
	 * Initialise mesh options.
	 *
	 * @param settings
	 * @throws Exception
	 */
	public MeshOptions init(MeshTestSetting settings) throws Exception {
		meshOptions = new MeshOptions();

		if (settings == null) {
			throw new RuntimeException("Settings could not be found. Did you forgot to add the @MeshTestSetting annotation to your test?");
		}
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
		httpOptions.setPort(port);
		if (settings.ssl()) {
			httpOptions.setSsl(true);
			httpOptions.setCertPath("src/test/resources/ssl/cert.pem");
			httpOptions.setKeyPath("src/test/resources/ssl/key.pem");
		}

		MonitoringConfig monitoringOptions = meshOptions.getMonitoringOptions();
		monitoringOptions.setPort(monitoringPort);

		// The database provider will switch to in memory mode when no directory has been specified.
		GraphStorageOptions storageOptions = meshOptions.getStorageOptions();

		String graphPath = null;
		if (!settings.inMemoryDB() || settings.clusterMode()) {
			graphPath = "target/graphdb_" + UUIDUtil.randomUUID();
			File directory = new File(graphPath);
			directory.deleteOnExit();
			directory.mkdirs();
		}
		if (!settings.inMemoryDB() && settings.startStorageServer()) {
			storageOptions.setStartServer(true);
		}
		// Increase timeout to high load during testing
		ElasticSearchOptions searchOptions = meshOptions.getSearchOptions();
		searchOptions.setTimeout(10_000L);
		storageOptions.setDirectory(graphPath);
		storageOptions.setSynchronizeWrites(true);

		switch (settings.elasticsearch()) {
		case CONTAINER:
		case UNREACHABLE:
			elasticsearch = new ElasticsearchContainer();
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
		case CONTAINER_TOXIC:
			network = Network.newNetwork();
			elasticsearch = new ElasticsearchContainer().withNetwork(network);
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
			searchOptions.setStartEmbedded(true);
			break;
		case NONE:
			searchOptions.setUrl(null);
			searchOptions.setStartEmbedded(false);
			break;
		case TRACKING:
			System.setProperty(TrackingSearchProvider.TEST_PROPERTY_KEY, "true");
			searchOptions.setStartEmbedded(false);
			break;
		default:
			break;
		}

		if (settings.useKeycloak()) {
			keycloak = new KeycloakContainer("/keycloak/realm.json").waitingFor(Wait.forHttp("/auth/realms/master-test"));
			if (!keycloak.isRunning()) {
				keycloak.start();
			}
			String authUrl = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080) + "/auth";
			String realmName = "master-test";
			String key = KeycloakUtils.loadPublicKey(realmName, authUrl);
			authOptions.setPublicKey(key);
		}
		settings.optionChanger().changer.accept(meshOptions);
		optionChanger.accept(meshOptions);
		return meshOptions;
	}

	private void initFolders(MeshOptions meshOptions) throws IOException {
		String tmpDir = newFolder("tmpDir");
		meshOptions.setTempDirectory(tmpDir);

		String uploads = newFolder("testuploads");
		meshOptions.getUploadOptions().setDirectory(uploads);

		String targetUploadTmpDir = newFolder("uploadTmpDir");
		meshOptions.getUploadOptions().setTempDirectory(targetUploadTmpDir);

		String imageCacheDir = newFolder("image_cache");
		meshOptions.getImageOptions().setImageCacheDirectory(imageCacheDir);

		String backupPath = newFolder("backups");
		meshOptions.getStorageOptions().setBackupDirectory(backupPath);

		String exportPath = newFolder("exports");
		meshOptions.getStorageOptions().setExportDirectory(exportPath);

		String plugindirPath = newFolder("plugins");
		meshOptions.setPluginDirectory(plugindirPath);
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
		meshDagger = DaggerMeshComponent.builder()
			.configuration(options)
			.searchProviderType(settings.elasticsearch().toSearchProviderType())
			.build();
		dataProvider = new TestDataProvider(settings.testSize(), meshDagger.boot(), meshDagger.database(), meshDagger.batchProvider());
		if (meshDagger.searchProvider() instanceof TrackingSearchProvider) {
			trackingSearchProvider = meshDagger.trackingSearchProvider();
		}
		try {
			mesh = Mesh.create(options);
			mesh.setMeshInternal(meshDagger);
			meshDagger.boot().init(mesh, false, options, null);
			vertx = meshDagger.boot().vertx();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public MonitoringRestClient getMonitoringClient() {
		return monitoringClient;
	}

	public MeshRestClient getClient() {
		return getClient("v" + CURRENT_API_VERSION);
	}

	public MeshRestClient getClient(String version) {
		return clients.get(version);
	}

	public static KeycloakContainer getKeycloak() {
		return keycloak;
	}

	public MeshOptions getOptions() {
		return meshOptions;
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
		Objects.requireNonNull(idleConsumer, "Call #listenToSearchIdleEvent first");
		ElasticsearchProcessVerticle verticle = getElasticSearchVerticle();
		try {
			verticle.flush().blockingAwait();
			idleLatch = new CountDownLatch(1);
			boolean success = idleLatch.await(30, TimeUnit.SECONDS);
			if (!success) {
				throw new RuntimeException("Timed out while waiting for search idle event");
			}
			verticle.refresh().blockingAwait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public ElasticsearchProcessVerticle getElasticSearchVerticle() {
		return ((BootstrapInitializerImpl) meshDagger.boot()).loader.get().getSearchVerticle();
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
}
