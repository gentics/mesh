package com.gentics.mesh.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.MeshRestClient;
import com.gentics.mesh.rest.MeshRestClientHttpException;

public abstract class AbstractRestVerticleTest extends AbstractDBTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractRestVerticleTest.class);

	@Autowired
	private I18NService i18n;

	protected Vertx vertx;

	private int port;

	private static final Integer CI_TIMEOUT_SECONDS = 10;

	private static final Integer DEV_TIMEOUT_SECONDS = 100000;

	private MeshRestClient client;

	@Autowired
	private RouterStorage routerStorage;

	@Before
	public void setupVerticleTest() throws Exception {
		setupData();
		port = com.gentics.mesh.test.TestUtil.getRandomPort();
		vertx = Mesh.vertx();

		routerStorage.addProjectRouter(DemoDataProvider.PROJECT_NAME);

		AbstractWebVerticle verticle = getVerticle();
		// Inject spring config
		verticle.setSpringConfig(springConfig);
		JsonObject config = new JsonObject();
		config.put("port", port);
		EventLoopContext context = ((VertxInternal) vertx).createEventLoopContext("test", config, Thread.currentThread().getContextClassLoader());
		verticle.init(vertx, context);
		verticle.start();
		verticle.registerEndPoints();
		client = new MeshRestClient("localhost", getPort());
		client.setLogin(user().getUsername(), data().getUserInfo().getPassword());
		resetClientSchemaStorage();

	}

	protected void resetClientSchemaStorage() throws IOException {
		getClient().getClientSchemaStorage().clear();
		for (SchemaContainer container : data().getSchemaContainers().values()) {
			getClient().getClientSchemaStorage().addSchema(container.getSchema());
		}
	}

	public abstract AbstractWebVerticle getVerticle();

	@After
	public void tearDown() throws Exception {
		if (client != null) {
			client.close();
		}
	}

	public int getPort() {
		return port;
	}

	public MeshRestClient getClient() {
		return client;
	}

	protected void latchFor(Future<?> future) {
		CountDownLatch latch = new CountDownLatch(1);
		future.setHandler(rh -> {
			latch.countDown();
		});
		try {
			assertTrue("The timeout of the latch was reached.", latch.await(getTimeout(), TimeUnit.SECONDS));
		} catch (UnknownHostException | InterruptedException e) {
			e.printStackTrace();
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

	public void assertEqualsSanitizedJson(String msg, String expectedJson, String unsanitizedResponseJson) {
		String sanitizedJson = unsanitizedResponseJson.replaceAll("uuid\":\"[^\"]*\"", "uuid\":\"uuid-value\"");
		assertEquals(msg, expectedJson, sanitizedJson);
	}

	protected void expectMessageResponse(String i18nKey, Future<GenericMessageResponse> responseFuture, String... i18nParams) {
		assertTrue("The given future has not yet completed.", responseFuture.isComplete());
		Locale en = Locale.ENGLISH;
		String message = i18n.get(en, i18nKey, i18nParams);
		assertEquals("The response message does not match.", message, responseFuture.result().getMessage());
	}

	protected void expectMessage(Future<?> future, HttpResponseStatus status, String message) {
		assertTrue("We expected the future to have failed but it succeeded.", future.failed());
		assertNotNull(future.cause());

		if (future.cause() instanceof MeshRestClientHttpException) {
			MeshRestClientHttpException exception = ((MeshRestClientHttpException) future.cause());
			assertEquals(status.code(), exception.getStatusCode());
			assertEquals(status.reasonPhrase(), exception.getMessage());
			assertNotNull(exception.getResponseMessage());
			assertEquals(message, exception.getResponseMessage().getMessage());
		} else {
			future.cause().printStackTrace();
			fail("Unhandled exception");
		}
	}

	protected void expectException(Future<?> future, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = i18n.get(en, bodyMessageI18nKey, i18nParams);
		expectMessage(future, status, message);
	}

	protected void assertSuccess(Future<?> future) {
		if (future.cause() != null) {
			future.cause().printStackTrace();
		}
		assertTrue("The future failed with error {" + (future.cause() == null ? "Unknown error" : future.cause().getMessage()) + "}",
				future.succeeded());
	}
}
