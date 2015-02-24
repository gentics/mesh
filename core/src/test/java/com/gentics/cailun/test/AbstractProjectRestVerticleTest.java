package com.gentics.cailun.test;

import static org.junit.Assert.fail;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractProjectRestVerticleTest {

	private static final Integer DEFAULT_TIMEOUT_SECONDS = 10;

	private HttpClient client;

	private AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();

	@Autowired
	protected CaiLunSpringConfiguration springConfig;

	@Autowired
	protected DummyDataProvider dataProvider;

	private CountDownLatch latch;
	protected Vertx vertx;
	private int port;

	@Before
	public void setup() throws Exception {
		purgeDatabase();
		vertx = springConfig.vertx();
		dataProvider.setup();
		springConfig.routerStorage().addProjectRouter(DummyDataProvider.PROJECT_NAME);
		client = vertx.createHttpClient(new HttpClientOptions());
		latch = new CountDownLatch(1);
		throwable.set(null);

		AbstractProjectRestVerticle verticle = getVerticle();
		// Inject spring config
		verticle.setSpringConfig(springConfig);
		JsonObject config = new JsonObject();
		port = TestUtil.getRandomPort();
		config.put("port", port);
		EventLoopContext context = ((VertxInternal) vertx).createEventLoopContext("test", config, Thread.currentThread().getContextClassLoader());
		verticle.init(vertx, context);
		verticle.start();
		verticle.registerEndPoints();
	}

	public abstract AbstractProjectRestVerticle getVerticle();

	@After
	public void tearDown() throws Exception {
		if (client != null) {
			client.close();
		}
	}

	protected void purgeDatabase() {
		try (Transaction tx = springConfig.graphDatabase().beginTx()) {
			for (Node node : springConfig.getGraphDatabaseService().getAllNodes()) {
				for (Relationship rel : node.getRelationships()) {
					rel.delete();
				}
				node.delete();
			}
			tx.success();
		}
	}

	protected void testAuthenticatedRequest(HttpMethod method, String path, int statusCode, String statusMessage, String responseBody)
			throws Exception {
		Consumer<HttpClientRequest> requestAction = request -> {
			String authStringEnc = DummyDataProvider.USER_JOE_USERNAME + ":" + DummyDataProvider.USER_JOE_PASSWORD;
			byte[] authEncBytes = Base64.encodeBase64(authStringEnc.getBytes());
			request.headers().add("Authorization", "Basic " + new String(authEncBytes));
		};
		testRequest(method, path, requestAction, null, statusCode, statusMessage, responseBody);

	}

	protected void testRequest(HttpMethod method, String path, int statusCode, String statusMessage) throws Exception {
		testRequest(method, path, null, statusCode, statusMessage, null);
	}

	protected void testRequest(HttpMethod method, String path, int statusCode, String statusMessage, String responseBody) throws Exception {
		testRequest(method, path, null, statusCode, statusMessage, responseBody);
	}

	protected void testRequest(HttpMethod method, String path, int statusCode, String statusMessage, Buffer responseBody) throws Exception {
		testRequestBuffer(method, path, null, null, statusCode, statusMessage, responseBody);
	}

	protected void testRequestWithContentType(HttpMethod method, String path, String contentType, int statusCode, String statusMessage)
			throws Exception {
		testRequest(method, path, req -> req.putHeader("content-type", contentType), statusCode, statusMessage, null);
	}

	protected void testRequestWithAccepts(HttpMethod method, String path, String accepts, int statusCode, String statusMessage) throws Exception {
		testRequest(method, path, req -> req.putHeader("accepts", accepts), statusCode, statusMessage, null);
	}

	protected void testRequestWithCookies(HttpMethod method, String path, String cookieHeader, int statusCode, String statusMessage) throws Exception {
		testRequest(method, path, req -> req.putHeader("cookie", cookieHeader), statusCode, statusMessage, null);
	}

	protected void testRequest(HttpMethod method, String path, Consumer<HttpClientRequest> requestAction, int statusCode, String statusMessage,
			String responseBody) throws Exception {
		testRequest(method, path, requestAction, null, statusCode, statusMessage, responseBody);
	}

	protected void testRequest(HttpMethod method, String path, Consumer<HttpClientRequest> requestAction,
			Consumer<HttpClientResponse> responseAction, int statusCode, String statusMessage, String responseBody) throws Exception {
		testRequestBuffer(method, path, requestAction, responseAction, statusCode, statusMessage, responseBody != null ? Buffer.buffer(responseBody)
				: null);
	}

	protected void testRequestBuffer(HttpMethod method, String path, Consumer<HttpClientRequest> requestAction,
			Consumer<HttpClientResponse> responseAction, int statusCode, String statusMessage, Buffer responseBodyBuffer) throws Exception {
		testRequestBuffer(client, method, port, path, requestAction, responseAction, statusCode, statusMessage, responseBodyBuffer);
	}

	protected void testRequestBuffer(HttpClient client, HttpMethod method, int port, String path, Consumer<HttpClientRequest> requestAction,
			Consumer<HttpClientResponse> responseAction, int statusCode, String statusMessage, Buffer responseBodyBuffer) throws Exception {

		HttpClientRequest req = client.request(method, port, "localhost", path, resp -> {
			assertEquals("The response status code did not match the expected one.", statusCode, resp.statusCode());
			assertEquals("The reponse status message did not match.", statusMessage, resp.statusMessage());
			if (responseAction != null) {
				responseAction.accept(resp);
			}
			if (responseBodyBuffer == null) {
				latch.countDown();
			} else {
				resp.bodyHandler(buff -> {
					assertEquals("The response body did not match the expected one.", responseBodyBuffer.toString(), buff.toString());
					latch.countDown();
				});
			}
		});
		if (requestAction != null) {
			requestAction.accept(req);
		}
		req.end();
		awaitCompletion();
	}

	/**
	 * Wait for the completion and latching of all latches. Check any thrown exception.
	 * 
	 * @throws InterruptedException
	 * @throws AssertionError
	 */
	private void awaitCompletion() throws InterruptedException, AssertionError {

		boolean allLatchesFree = latch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		if (throwable.get() != null) {
			throw (AssertionError) throwable.get();
		} else if (!allLatchesFree) {
			fail("Timeout of {" + DEFAULT_TIMEOUT_SECONDS + "} seconds reached.");
		}
	}

	private void assertEquals(String message, Object expected, Object actual) {
		try {
			Assert.assertEquals(message, expected, actual);
		} catch (AssertionError e) {
			// Only store the first encountered exception
			if (throwable.get() == null) {
				throwable.set(e);
			}
		}
	}

}
