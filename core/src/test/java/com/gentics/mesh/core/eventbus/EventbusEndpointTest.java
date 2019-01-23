
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
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class EventbusEndpointTest extends AbstractMeshTest {

	private WebSocket ws;

	@Before
	public void setupEventbus() throws Exception {
		CompletableFuture<WebSocket> fut = new CompletableFuture<>();
		client().eventbus(ws -> {
			fut.complete(ws);
		}, fh -> {
			fh.printStackTrace();
			fail("Could not connect to eventbus.");
		});
		ws = fut.get(4, TimeUnit.SECONDS);
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
		JsonObject msg = new JsonObject().put("type", "register").put("address", allowedAddress);
		ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

		// Handle msgs
		ws.handler(buff -> {
			String str = buff.toString();
			System.out.println("msg:" + str);
			JsonObject received = new JsonObject(str);
			JsonObject rec = received.getJsonObject("body");
			String value = rec.getString("test");
			assertEquals("someValue", value);
			async.complete();
		});

		Thread.sleep(1000);
		Mesh.vertx().eventBus().send(allowedAddress, new JsonObject().put("test", "someValue"));

	}

	@Test(timeout = 4_000)
	public void testNodeDeleteEvent(TestContext context) throws Exception {
		Async async = context.async();

		// Register
		JsonObject msg = new JsonObject().put("type", "register").put("address", NODE_DELETED);
		ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

		// Handle msgs
		ws.handler(buff -> {
			String str = buff.toString();
			JsonObject received = new JsonObject(str);
			context.assertNotNull(received.getJsonObject("body").getString("uuid"));
			context.assertEquals("content", received.getJsonObject("body").getString("schemaName"));
			context.assertNull(received.getJsonObject("body").getString("languageTag"));
			async.complete();
		});
		call(() -> client().deleteNode(PROJECT_NAME, contentUuid()));
	}

	@Test(timeout = 4_000)
	public void testNodeDeleteLanguageEvent(TestContext context) throws Exception {
		Async async = context.async();

		// Register
		JsonObject msg = new JsonObject().put("type", "register").put("address", NODE_DELETED);
		ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

		// Handle msgs
		ws.handler(buff -> {
			String str = buff.toString();
			JsonObject received = new JsonObject(str);
			System.out.println(received.encodePrettily());
			context.assertNotNull(received.getJsonObject("body").getString("uuid"));
			context.assertEquals("content", received.getJsonObject("body").getString("schemaName"));
			context.assertEquals("en", received.getJsonObject("body").getString("languageTag"));
			async.complete();
		});
		call(() -> client().deleteNode(PROJECT_NAME, contentUuid(), "en"));
	}

	@Test(timeout = 4_000)
	public void testNodeUpdateEvent(TestContext context) throws Exception {
		Async async = context.async();

		// Register
		JsonObject msg = new JsonObject().put("type", "register").put("address", NODE_UPDATED);
		ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

		// Handle msgs
		ws.handler(buff -> {
			String str = buff.toString();
			JsonObject received = new JsonObject(str);
			assertNotNull(received.getJsonObject("body").getString("uuid"));
			assertEquals("content", received.getJsonObject("body").getString("schemaName"));
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

		// Register
		JsonObject msg = new JsonObject().put("type", "register").put("address", "custom.myEvent");
		ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

		// Handle msgs
		ws.handler(buff -> {
			String str = buff.toString();
			JsonObject received = new JsonObject(str);
			String body = received.getString("body");
			assertEquals("someText", body);
			asyncRec.complete();
		});

		// Send msg
		msg = new JsonObject().put("type", "publish").put("address", "custom.myEvent").put("body", "someText");
		ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));
	}

	@Test(timeout = 4_000)
	public void testRegisterToEventbus(TestContext context) throws Exception {
		Async asyncRec = context.async();

		// Register
		JsonObject msg = new JsonObject().put("type", "register").put("address", "custom.address");
		ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

		// Handle msgs
		ws.handler(buff -> {
			String str = buff.toString();
			JsonObject received = new JsonObject(str);
			Object rec = received.getValue("body");
			System.out.println("Handler:" + rec.toString());
			asyncRec.complete();
		});

		// Send msg
		msg = new JsonObject().put("type", "publish").put("address", "custom.address").put("body", "someText");
		ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));
	}

}
