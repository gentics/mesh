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

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializerImpl;
import com.gentics.mesh.core.cache.PermissionStore;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.ElasticSearchOptions;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.impl.MeshFactoryImpl;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.search.DummySearchProvider;
import com.gentics.mesh.test.TestDataProvider;
import com.gentics.mesh.test.TestFullDataProvider;
import com.gentics.mesh.test.TestTinyDataProvider;
import com.gentics.mesh.test.performance.TestUtils;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MeshTestContext extends TestWatcher {

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
			removeDataDirectory();
			init(settings.useElasticsearch());
			initDagger(settings.useTinyDataset());
			setupData();
			if (settings.useElasticsearch()) {
				setupIndexHandlers();
			}
			if (settings.startServer()) {
				setupRestEndpoints();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void finished(Description description) {
		try {
			MeshTestSetting settings = getSettings(description);
			cleanupFolders();
			if (settings.startServer()) {
				undeployAndReset();
				closeClient();
			}
			if (settings.useElasticsearch()) {
				meshDagger.searchProvider().clear();
				removeDataDirectory();
			} else {
				meshDagger.dummySearchProvider().clear();
			}
			resetDatabase();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void removeDataDirectory() throws IOException {
		FileUtils.deleteDirectory(new File("data"));
	}

	protected void setupIndexHandlers() throws Exception {
		// We need to call init() again in order create missing indices for the created test data
		for (IndexHandler handler : meshDagger.indexHandlerRegistry().getHandlers()) {
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

		routerStorage.addProjectRouter(TestFullDataProvider.PROJECT_NAME);
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
		try (NoTx trx = db().noTx()) {
			client = MeshRestClient.create("localhost", getPort(), vertx);
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
	 */
	private void resetDatabase() {
		BootstrapInitializerImpl.clearReferences();
		long start = System.currentTimeMillis();
		MeshInternal.get().database().clear();
		long duration = System.currentTimeMillis() - start;
		log.info("Clearing DB took {" + duration + "} ms.");
		if (dummySearchProvider != null) {
			dummySearchProvider.reset();
		}
	}

	private void cleanupFolders() throws IOException {
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getImageOptions().getImageCacheDirectory()));
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory()));
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getUploadOptions().getTempDirectory()));
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
	 * @param useElasticSearch
	 * @throws IOException
	 */
	public void init(boolean useElasticSearch) throws IOException {
		MeshFactoryImpl.clear();
		MeshOptions options = new MeshOptions();

		String uploads = "target/testuploads_" + UUIDUtil.randomUUID();
		File uploadDir = new File(uploads);
		FileUtils.deleteDirectory(uploadDir);
		uploadDir.mkdirs();
		options.getUploadOptions().setDirectory(uploads);

		String targetTmpDir = "target/tmp_" + UUIDUtil.randomUUID();
		File tmpDir = new File(targetTmpDir);
		FileUtils.deleteDirectory(tmpDir);
		tmpDir.mkdirs();
		options.getUploadOptions().setTempDirectory(targetTmpDir);

		String imageCacheDir = "target/image_cache_" + UUIDUtil.randomUUID();
		File cacheDir = new File(imageCacheDir);
		FileUtils.deleteDirectory(cacheDir);
		cacheDir.mkdirs();
		options.getImageOptions().setImageCacheDirectory(imageCacheDir);

		options.getHttpServerOptions().setPort(TestUtils.getRandomPort());
		// The database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory(null);

		ElasticSearchOptions searchOptions = new ElasticSearchOptions();
		if (useElasticSearch) {
			searchOptions.setDirectory("target/elasticsearch_data_" + System.currentTimeMillis());
		} else {
			searchOptions.setDirectory(null);
		}
		searchOptions.setHttpEnabled(true);
		options.setSearchOptions(searchOptions);
		Mesh.mesh(options);
	}

	/**
	 * Initialise the mesh dagger context and inject the dependencies within the test.
	 */
	public void initDagger(boolean tiny) {
		log.info("Initializing dagger context");
		meshDagger = MeshInternal.create();
		if (tiny) {
			dataProvider = new TestTinyDataProvider(meshDagger.boot(), meshDagger.database());
		} else {
			dataProvider = new TestFullDataProvider(meshDagger.boot(), meshDagger.database());
		}
		routerStorage = meshDagger.routerStorage();
		if (meshDagger.searchProvider() instanceof DummySearchProvider) {
			dummySearchProvider = meshDagger.dummySearchProvider();
		}
		//		searchProvider = meshDagger.searchProvider();
		//		schemaStorage = meshDagger.serverSchemaStorage();
		//		boot = meshDagger.boot();
		Database db = meshDagger.database();
		new DatabaseHelper(db).init();
	}

	public MeshRestClient getClient() {
		return client;
	}
}
