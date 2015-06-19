package com.gentics.mesh.test;

import static org.junit.Assert.assertEquals;
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
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.common.response.GenericMessageResponse;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.util.JsonUtils;

public abstract class AbstractRestVerticleTest extends AbstractDBTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractRestVerticleTest.class);

	@Autowired
	private I18NService i18n;

	protected Vertx vertx;

	private int port;

	private static final Integer CI_TIMEOUT_SECONDS = 10;

	private static final Integer DEV_TIMEOUT_SECONDS = 100000;

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
		port = com.gentics.mesh.test.TestUtil.getRandomPort();
		vertx = springConfig.vertx();
		client = vertx.createHttpClient(new HttpClientOptions());
		latch = new CountDownLatch(1);
		throwable.set(null);

		routerStorage.addProjectRouter(DemoDataProvider.PROJECT_NAME);

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

	public String request(UserInfo info, HttpMethod method, String path, int statusCode, String statusMessage, String requestBody) throws Exception {

		if (requestBody != null) {
			Map<String, String> extraHeaders = new HashMap<>();
			Buffer buffer = Buffer.buffer();
			buffer.appendString(requestBody);
			extraHeaders.put("content-length", String.valueOf(buffer.length()));
			extraHeaders.put("content-type", "application/json");
			return request(info, method, path, statusCode, statusMessage, buffer, extraHeaders);
		} else {
			return request(info, method, path, statusCode, statusMessage, null, null);
		}
	}

	protected String request(UserInfo info, HttpMethod method, String path, int statusCode, String statusMessage, Buffer requestBuffer,
			Map<String, String> extraHeaders) throws Exception {
		// Reset the latch etc.
		latch = new CountDownLatch(1);
		throwable.set(null);

		Thread.sleep(100);

		Consumer<HttpClientRequest> requestAction = request -> {
			String authStringEnc = info.getUser().getUsername() + ":" + info.getPassword();
			byte[] authEncBytes = Base64.encodeBase64(authStringEnc.getBytes());
			request.headers().add("Authorization", "Basic " + new String(authEncBytes));
			request.headers().add("Accept", "application/json");
			if (extraHeaders != null) {
				for (Entry<String, String> entry : extraHeaders.entrySet()) {
					request.headers().set(entry.getKey(), entry.getValue());
				}
			}
			if (requestBuffer != null) {
				request.write(requestBuffer);
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
			blockingAssertEquals("The response status code did not match the expected one.", statusCode, resp.statusCode());
			blockingAssertEquals("The reponse status message did not match.", statusMessage, resp.statusMessage());
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
	 * @throws UnknownHostException
	 */
	private void awaitCompletion() throws InterruptedException, AssertionError, UnknownHostException {

		int timeout = getTimeout();
		boolean allLatchesFree = latch.await(timeout, TimeUnit.SECONDS);
		if (throwable.get() != null) {
			throw (AssertionError) throwable.get();
		} else if (!allLatchesFree) {
			fail("Timeout of {" + timeout + "} seconds reached.");
		}
	}

	public int getTimeout() throws UnknownHostException {
		int timeout = DEV_TIMEOUT_SECONDS;
		if (TestUtil.isHost("jenkins.office")) {
			timeout = CI_TIMEOUT_SECONDS;
		}
		log.info("Using test timeout of {" + timeout + "} seconds for host {" + TestUtil.getHostname() + "}");
		return timeout;
	}

	private void blockingAssertEquals(String message, Object expected, Object actual) {
		try {
			assertEquals(message, expected, actual);
		} catch (AssertionError e) {
			// Only store the first encountered exception
			if (throwable.get() == null) {
				throwable.set(e);
			}
		}
	}

	public void assertEqualsSanitizedJson(String msg, String expectedJson, String unsanitizedResponseJson) {
		String sanitizedJson = unsanitizedResponseJson.replaceAll("uuid\":\"[^\"]*\"", "uuid\":\"uuid-value\"");
		assertEquals(msg, expectedJson, sanitizedJson);
	}

	protected void expectMessageResponse(String i18nKey, String response, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = i18n.get(en, i18nKey, i18nParams);
		GenericMessageResponse responseObject = new GenericMessageResponse(message);
		String json = JsonUtils.toJson(responseObject);
		assertEquals("The response does not match.", json, response);
	}

}
