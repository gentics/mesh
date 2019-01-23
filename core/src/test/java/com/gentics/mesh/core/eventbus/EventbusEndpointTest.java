
package com.gentics.mesh.core.eventbus;

import static com.gentics.mesh.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.MeshEvent.MESH_MIGRATION;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.gentics.mesh.assertj.MeshAssertions;
import com.gentics.mesh.rest.client.MeshWebsocket;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class EventbusEndpointTest extends AbstractMeshTest {

	private MeshWebsocket ws;

	@Before
	public void setupEventbus() throws Exception {
		ws = client().eventbus();
		// Wait for initial connection
		ws.connections().blockingFirst();
	}

	@After
	public void closeEventBus() {
		if (ws != null) {
			ws.close();
		}
	}

	@Test(timeout = 4_000)
	public void testExternalEventbusMessage(TestContext context) throws Exception {

		Async async = context.async();
		String allowedAddress = MESH_MIGRATION.address;

		// Register
		ws.registerEvents(allowedAddress);

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			MeshAssertions.assertThat(event.getBodyAsJson().getString("test")).isEqualTo("").isEqualTo("someValue");
			async.complete();
		});

		Thread.sleep(1000);
		Mesh.vertx().eventBus().send(allowedAddress, new JsonObject().put("test", "someValue"));
	}

	@Test(timeout = 4_000)
	public void testNodeDeleteEvent(TestContext context) throws Exception {
		Async async = context.async();

		ws.registerEvents(NODE_DELETED);

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			JSONObject body = event.getBodyAsJson();
			context.assertNotNull(body.getString("uuid"));
			context.assertEquals("content", body.getString("schemaName"));
			context.assertNull(body.getString("languageTag"));
			async.complete();
		});
		call(() -> client().deleteNode(PROJECT_NAME, contentUuid()));
	}

	@Test(timeout = 4_000)
	public void testNodeDeleteLanguageEvent(TestContext context) throws Exception {
		Async async = context.async();

		ws.registerEvents(NODE_DELETED);

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			JSONObject received = event.getBodyAsJson();
			context.assertNotNull(received.getJSONObject("body").getString("uuid"));
			context.assertEquals("content", received.getJSONObject("body").getString("schemaName"));
			context.assertEquals("en", received.getJSONObject("body").getString("languageTag"));
			async.complete();
		});
		call(() -> client().deleteNode(PROJECT_NAME, contentUuid(), "en"));
	}

	@Test(timeout = 4_000)
	public void testNodeUpdateEvent(TestContext context) throws Exception {
		Async async = context.async();

		// Register
		ws.registerEvents(NODE_UPDATED);

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			JSONObject received = event.getBodyAsJson();
			assertNotNull(received.getJSONObject("body").getString("uuid"));
			assertEquals("content", received.getJSONObject("body").getString("schemaName"));
			async.complete();
		});

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		NodeUpdateRequest request = new NodeUpdateRequest();
		request.getFields().put("slug", FieldUtil.createStringField("blub"));
		request.setVersion(response.getVersion());
		request.setLanguage("en");
		call(() -> client().updateNode(PROJECT_NAME, contentUuid(), request));

		NodeResponse response2 = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		assertNotEquals(response.getVersion(), response2.getVersion());
	}

	@Test
	public void testCustomEventHandling(TestContext context) {
		Async asyncRec = context.async();

		ws.registerEvents("custom.myEvent");

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			String body = event.getBodyAsString();
			assertEquals("someText", body);
			asyncRec.complete();
		});

		// Send msg
		ws.publishEvent("custom.myEvent", "someText");
	}
}
