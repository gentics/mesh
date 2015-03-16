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
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.etc.RouterStorage;

public abstract class AbstractRestVerticleTest extends AbstractDBTest {

	protected Vertx vertx;

	private int port;

	private static final Integer DEFAULT_TIMEOUT_SECONDS = 200;

	private HttpClient client;

	private AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();

	private CountDownLatch latch;

	protected UserInfo info;

	@Autowired
	private RouterStorage routerStorage;

	@Before
	public void setupVerticleTest() throws Exception {
		setupData();
		info = data().getUserInfo();
		port = TestUtil.getRandomPort();
		vertx = springConfig.vertx();
		client = vertx.createHttpClient(new HttpClientOptions());
		latch = new CountDownLatch(1);
		throwable.set(null);

		routerStorage.addProjectRouter(TestDataProvider.PROJECT_NAME);

		AbstractRestVerticle verticle = getVerticle();
		// Inject spring config
		verticle.setSpringConfig(springConfig);
		JsonObject config = new JsonObject();
		config.put("port", port);
		EventLoopContext context = ((VertxInternal) vertx).createEventLoopContext("test", config, Thread.currentThread().getContextClassLoader());
		verticle.init(vertx, context);
		verticle.start();
		verticle.registerEndPoints();
	}

	public abstract AbstractRestVerticle getVerticle();

	@After
	public void tearDown() throws Exception {
		if (client != null) {
			client.close();
		}
	}

	public int getPort() {
		return port;
	}

	protected String request(UserInfo info, HttpMethod method, String path, int statusCode, String statusMessage) throws Exception {
		return request(info, method, path, statusCode, statusMessage, null);
	}

	protected String request(UserInfo info, HttpMethod method, String path, int statusCode, String statusMessage, String requestBody)
			throws Exception {
		// Reset the latch etc.
		latch = new CountDownLatch(1);
		throwable.set(null);

		Consumer<HttpClientRequest> requestAction = request -> {
			String authStringEnc = info.getUser().getUsername() + ":" + info.getPassword();
			byte[] authEncBytes = Base64.encodeBase64(authStringEnc.getBytes());
			request.headers().add("Authorization", "Basic " + new String(authEncBytes));
			request.headers().add("Accept", "application/json");
			if (requestBody != null) {
				Buffer buffer = Buffer.buffer();
				buffer.appendString(requestBody);
				request.headers().set("content-length", String.valueOf(buffer.length()));
				request.headers().set("content-type", "application/json");
				request.write(buffer);
			}
		};
		return request(method, path, requestAction, null, statusCode, statusMessage);

	}

	protected String request(HttpMethod method, String path, Consumer<HttpClientRequest> requestAction, Consumer<HttpClientResponse> responseAction,
			int statusCode, String statusMessage) throws Exception {
		return request(client, method, port, path, requestAction, responseAction, statusCode, statusMessage);
	}

	protected String request(HttpClient client, HttpMethod method, int port, String path, Consumer<HttpClientRequest> requestAction,
			Consumer<HttpClientResponse> responseAction, int statusCode, String statusMessage) throws Exception {

		AtomicReference<String> responseBody = new AtomicReference<String>(null);
		HttpClientRequest req = client.request(method, port, "localhost", path, resp -> {
			assertEquals("The response status code did not match the expected one.", statusCode, resp.statusCode());
			assertEquals("The reponse status message did not match.", statusMessage, resp.statusMessage());
			if (responseAction != null) {
				responseAction.accept(resp);
			}
			resp.bodyHandler(buff -> {
				responseBody.set(buff.toString());
				latch.countDown();
			});
		});
		if (requestAction != null) {
			requestAction.accept(req);
		}
		req.end();
		awaitCompletion();
		return responseBody.get();
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

	public void assertEqualsSanitizedJson(String msg, String expectedJson, String unsanitizedResponseJson) {
		String sanitizedJson = unsanitizedResponseJson.replaceAll("uuid\":\"[^\"]*\"", "uuid\":\"uuid-value\"");
		org.junit.Assert.assertEquals(msg, expectedJson, sanitizedJson);
	}

}
