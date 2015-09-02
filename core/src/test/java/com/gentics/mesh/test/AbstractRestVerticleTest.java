package com.gentics.mesh.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.MeshRestClient;
import com.gentics.mesh.rest.MeshRestClientHttpException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractRestVerticleTest extends AbstractDBTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractRestVerticleTest.class);

	@Autowired
	private I18NService i18n;

	protected Vertx vertx;

	private int port;

	private MeshRestClient client;

	@Autowired
	private RouterStorage routerStorage;

	@Autowired
	protected DummySearchProvider searchProvider;

	@Before
	public void setupVerticleTest() throws Exception {
		setupData();
		port = com.gentics.mesh.test.TestUtil.getRandomPort();
		vertx = Mesh.vertx();

		routerStorage.addProjectRouter(DemoDataProvider.PROJECT_NAME);

		List<AbstractWebVerticle> vertices = getVertices();

		JsonObject config = new JsonObject();
		config.put("port", port);
		EventLoopContext context = ((VertxInternal) vertx).createEventLoopContext("test", config, Thread.currentThread().getContextClassLoader());

		for (AbstractWebVerticle verticle : vertices) {
			// Inject spring config
			verticle.setSpringConfig(springConfig);
			verticle.init(vertx, context);
			verticle.start();
			verticle.registerEndPoints();
		}
		client = new MeshRestClient("localhost", getPort());
		client.setLogin(user().getUsername(), data().getUserInfo().getPassword());
		resetClientSchemaStorage();
	}

	@After
	public void cleanup() {
		searchProvider.reset();
		BootstrapInitializer.clearReferences();
		// databaseService.getDatabase().clear();
		databaseService.getDatabase().reset();
	}

	protected void resetClientSchemaStorage() throws IOException {
		getClient().getClientSchemaStorage().clear();
		for (SchemaContainer container : data().getSchemaContainers().values()) {
			getClient().getClientSchemaStorage().addSchema(container.getSchema());
		}
	}

	public abstract List<AbstractWebVerticle> getVertices();

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

}
