package com.gentics.cailun.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gentics.cailun.etc.CaiLunSpringConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class AbstractVerticleTest {

	private HttpClient client;

	@Autowired
	protected CaiLunSpringConfiguration springConfig;

	@Before
	public void setup() throws Exception {
		client = springConfig.vertx().createHttpClient(new HttpClientOptions());
	}

	protected void testAuthenticatedRequest(HttpMethod method, String path, int statusCode, String statusMessage) throws Exception {
		Consumer<HttpClientRequest> requestAction = request -> {
			String authStringEnc = DummyDataProvider.USER_JOE_USERNAME + ":" + DummyDataProvider.USER_JOE_PASSWORD;
			byte[] authEncBytes = Base64.encodeBase64(authStringEnc.getBytes());
			request.headers().add("Authorization", "Basic " + new String(authEncBytes));
		};

		Consumer<HttpClientResponse> responseAction = resp -> {
			String reqTime = resp.headers().get("x-response-time");
			assertNotNull(reqTime);
			assertTrue(reqTime.endsWith("ms"));
			String time = reqTime.substring(0, reqTime.length() - 2);
			Integer dur = Integer.valueOf(time);
			assertTrue(dur >= 250);
		};
		String responseBody = null;
		testRequest(method, path, requestAction, responseAction, statusCode, statusMessage, responseBody);

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
		testRequestBuffer(client, method, 8080, path, requestAction, responseAction, statusCode, statusMessage, responseBodyBuffer);
	}

	protected void testRequestBuffer(HttpClient client, HttpMethod method, int port, String path, Consumer<HttpClientRequest> requestAction,
			Consumer<HttpClientResponse> responseAction, int statusCode, String statusMessage, Buffer responseBodyBuffer) throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		HttpClientRequest req = client.request(method, port, "localhost", path, resp -> {
			assertEquals(statusCode, resp.statusCode());
			assertEquals(statusMessage, resp.statusMessage());
			if (responseAction != null) {
				responseAction.accept(resp);
			}
			if (responseBodyBuffer == null) {
				latch.countDown();
			} else {
				resp.bodyHandler(buff -> {
					assertEquals(responseBodyBuffer, buff);
					latch.countDown();
				});
			}
		});
		if (requestAction != null) {
			requestAction.accept(req);
		}
		req.end();
		awaitLatch(latch);
	}

	protected void awaitLatch(CountDownLatch latch) throws InterruptedException {
		assertTrue(latch.await(10, TimeUnit.SECONDS));
	}

}
