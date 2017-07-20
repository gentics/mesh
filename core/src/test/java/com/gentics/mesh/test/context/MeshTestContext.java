package com.gentics.mesh.test.context;

import static com.gentics.mesh.util.MeshAssert.failingLatch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.gentics.ferma.Tx;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializerImpl;
import com.gentics.mesh.core.cache.PermissionStore;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.crypto.KeyStoreHelper;
import com.gentics.mesh.dagger.DaggerTestMeshComponent;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.ElasticSearchOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.impl.MeshFactoryImpl;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.search.DummySearchProvider;
import com.gentics.mesh.test.TestDataProvider;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.performance.TestUtils;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MeshTestContext extends TestWatcher {

	private List<File> tmpFolders = new ArrayList<>();
	private static final Logger log = LoggerFactory.getLogger(MeshTestContext.class);
	private MeshComponent meshDagger;
	private TestDataProvider dataProvider;
	private DummySearchProvider dummySearchProvider;
	private Vertx vertx;

	protected int port;

	private MeshRestClient client;

	private RestAPIVerticle restVerticle;

	private NodeMigrationVerticle nodeMigrationVerticle;

	private List<String> deploymentIds = new ArrayList<>();
	private RouterStorage routerStorage;

	@Override
	protected void starting(Description description) {
		try {
			MeshTestSetting settings = getSettings(description);
			// Setup the dagger context and orientdb,es once
			if (description.isSuite()) {
				removeDataDirectory();
				init(settings);
				initDagger(settings.testSize());
			} else {
				if (!settings.inMemoryDB()) {
					DatabaseHelper.init(meshDagger.database());
				}
				setupData();
				if (settings.useElasticsearch()) {
					setupIndexHandlers();
				}
				if (settings.startServer()) {
					setupRestEndpoints();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void finished(Description description) {
		try {
			MeshTestSetting settings = getSettings(description);
			if (description.isSuite()) {
				removeDataDirectory();
			} else {
				cleanupFolders();
				if (settings.startServer()) {
					undeployAndReset();
					closeClient();
				}
				if (settings.useElasticsearch()) {
					meshDagger.searchProvider().clear();
				} else {
					meshDagger.dummySearchProvider().clear();
				}
				resetDatabase(settings);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void removeDataDirectory() throws IOException {
		FileUtils.deleteDirectory(new File("data"));
	}

	protected void setupIndexHandlers() throws Exception {
		// We need to call init() again in order create missing indices for the created test data
		for (IndexHandler<?> handler : meshDagger.indexHandlerRegistry().getHandlers()) {
			handler.init().await();
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

	private void setupRestEndpoints() throws Exception {
		Mesh.mesh().getOptions().getUploadOptions().setByteLimit(Long.MAX_VALUE);

		port = com.gentics.mesh.test.performance.TestUtils.getRandomPort();
		vertx = Mesh.vertx();

		routerStorage.addProjectRouter(TestDataProvider.PROJECT_NAME);
		JsonObject config = new JsonObject();
		config.put("port", port);

		// Start node migration verticle
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		CountDownLatch latch = new CountDownLatch(1);
		nodeMigrationVerticle = meshDagger.nodeMigrationVerticle();
		vertx.deployVerticle(nodeMigrationVerticle, options, rh -> {
			String deploymentId = rh.result();
			deploymentIds.add(deploymentId);
			latch.countDown();
		});
		failingLatch(latch);

		// Start rest verticle
		CountDownLatch latch2 = new CountDownLatch(1);
		restVerticle = MeshInternal.get().restApiVerticle();
		vertx.deployVerticle(restVerticle, new DeploymentOptions().setConfig(config), rh -> {
			String deploymentId = rh.result();
			deploymentIds.add(deploymentId);
			latch2.countDown();
		});
		failingLatch(latch2);

		// Setup the rest client
		try (Tx tx = db().tx()) {
			client = MeshRestClient.create("localhost", getPort(), Mesh.vertx());
			client.setLogin(getData().user().getUsername(), getData().getUserInfo().getPassword());
			client.login().toBlocking().value();
		}
		if (dummySearchProvider != null) {
			dummySearchProvider.clear();
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
		meshDagger.boot().createSearchIndicesAndMappings();
		dataProvider.setup();
		meshDagger.database().resetIntent();
	}

	private void undeployAndReset() throws Exception {
		for (String id : deploymentIds) {
			vertx.undeploy(id);
		}
	}

	private void closeClient() throws Exception {
		if (client != null) {
			client.close();
		}
	}

	/**
	 * Clear the test data.
	 * 
	 * @param settings
	 * @throws Exception
	 */
	private void resetDatabase(MeshTestSetting settings) throws Exception {
		BootstrapInitializerImpl.clearReferences();
		long start = System.currentTimeMillis();
		if (settings.inMemoryDB()) {
			MeshInternal.get().database().clear();
		} else {
			MeshInternal.get().database().stop();
			File dbDir = new File(Mesh.mesh().getOptions().getStorageOptions().getDirectory());
			FileUtils.deleteDirectory(dbDir);
			MeshInternal.get().database().setupConnectionPool();
		}
		long duration = System.currentTimeMillis() - start;
		log.info("Clearing DB took {" + duration + "} ms.");
		if (dummySearchProvider != null) {
			dummySearchProvider.reset();
		}
	}

	private void cleanupFolders() throws IOException {
		for (File folder : tmpFolders) {
			FileUtils.deleteDirectory(folder);
		}
		// if (Mesh.mesh().getOptions().getSearchOptions().getDirectory() != null) {
		// FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getSearchOptions().getDirectory()));
		// }
		PermissionStore.invalidate();
	}

	public TestDataProvider getData() {
		return dataProvider;
	}

	public DummySearchProvider getDummySearchProvider() {
		return dummySearchProvider;
	}

	/**
	 * Initialise mesh options.
	 * 
	 * @param settings
	 * @throws Exception
	 */
	public void init(MeshTestSetting settings) throws Exception {
		MeshFactoryImpl.clear();
		MeshOptions options = new MeshOptions();

		// Setup the keystore
		File keystoreFile = new File("target", "keystore_" + UUIDUtil.randomUUID() + ".jceks");
		keystoreFile.deleteOnExit();
		String keystorePassword = "finger";
		if (!keystoreFile.exists()) {
			KeyStoreHelper.gen(keystoreFile.getAbsolutePath(), keystorePassword);
		}
		options.getAuthenticationOptions().setKeystorePassword(keystorePassword);
		options.getAuthenticationOptions().setKeystorePath(keystoreFile.getAbsolutePath());

		String uploads = newFolder("testuploads");
		options.getUploadOptions().setDirectory(uploads);

		String targetTmpDir = newFolder("tmpdir");
		options.getUploadOptions().setTempDirectory(targetTmpDir);

		String imageCacheDir = newFolder("image_cache");
		options.getImageOptions().setImageCacheDirectory(imageCacheDir);

		String backupPath = newFolder("backups");
		options.getStorageOptions().setBackupDirectory(backupPath);

		String exportPath = newFolder("exports");
		options.getStorageOptions().setExportDirectory(exportPath);

		options.getHttpServerOptions().setPort(TestUtils.getRandomPort());
		// The database provider will switch to in memory mode when no directory has been specified.

		String graphPath = null;
		if (!settings.inMemoryDB()) {
			graphPath = "target/graphdb_" + UUIDUtil.randomUUID();
			File directory = new File(graphPath);
			directory.deleteOnExit();
			directory.mkdirs();
		}
		options.getStorageOptions().setDirectory(graphPath);
		ElasticSearchOptions searchOptions = new ElasticSearchOptions();
		if (settings.useElasticsearch()) {
			searchOptions.setDirectory("target/elasticsearch_data_" + System.currentTimeMillis());
		} else {
			searchOptions.setDirectory(null);
		}
		searchOptions.setHttpEnabled(settings.startESServer());
		options.setSearchOptions(searchOptions);
		Mesh.mesh(options);
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
		directory.mkdirs();
		tmpFolders.add(directory);
		return path;
	}

	/**
	 * Initialise the mesh dagger context and inject the dependencies within the test.
	 */
	public void initDagger(TestSize size) {
		log.info("Initializing dagger context");
		meshDagger = DaggerTestMeshComponent.create();
		MeshInternal.set(meshDagger);
		dataProvider = new TestDataProvider(size, meshDagger.boot(), meshDagger.database());
		routerStorage = meshDagger.routerStorage();
		if (meshDagger.searchProvider() instanceof DummySearchProvider) {
			dummySearchProvider = meshDagger.dummySearchProvider();
		}
		// searchProvider = meshDagger.searchProvider();
		// schemaStorage = meshDagger.serverSchemaStorage();
		// boot = meshDagger.boot();
		Database db = meshDagger.database();
		DatabaseHelper.init(db);
	}

	public MeshRestClient getClient() {
		return client;
	}
}
