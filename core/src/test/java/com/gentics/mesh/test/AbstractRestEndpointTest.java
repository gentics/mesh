package com.gentics.mesh.test;

import static com.gentics.mesh.util.MeshAssert.failingLatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.TestHelperMethods;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

/**
 * @deprecated Use {@link AbstractMeshTest} instead
 */
@Deprecated
public abstract class AbstractRestEndpointTest extends AbstractDBTest implements TestHelperMethods {

	protected Vertx vertx;

	protected int port;

	private MeshRestClient client;

	private RestAPIVerticle restVerticle;

	private NodeMigrationVerticle nodeMigrationVerticle;

	private List<String> deploymentIds = new ArrayList<>();

	@Before
	public void setupVerticleTest() throws Exception {
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
		try (NoTx trx = db.noTx()) {
			client = MeshRestClient.create("localhost", getPort(), vertx);
			client.setLogin(user().getUsername(), getUserInfo().getPassword());
			client.login().toBlocking().value();
		}
		if (dummySearchProvider != null) {
			dummySearchProvider.clear();
		}
	}

	public HttpClient createHttpClient() {
		HttpClientOptions options = new HttpClientOptions();
		options.setDefaultHost("localhost");
		options.setDefaultPort(port);
		HttpClient client = Mesh.vertx().createHttpClient(options);
		return client;
	}

	@After
	public void undeployAndReset() throws Exception {
		for (String id : deploymentIds) {
			vertx.undeploy(id);
		}
	}

	@After
	public void tearDown() throws Exception {
		if (client != null) {
			client.close();
		}
	}

	public int getPort() {
		return port;
	}

	public int port() {
		return port;
	}

	public MeshRestClient client() {
		return client;
	}

}
