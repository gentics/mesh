
package com.gentics.mesh.core.eventbus;

import static com.gentics.mesh.Events.EVENT_NODE_DELETED;
import static com.gentics.mesh.Events.EVENT_NODE_UPDATED;
import static com.gentics.mesh.Events.MESH_MIGRATION;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import io.vertx.ext.unit.Async;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class EventbusEndpointTest extends AbstractMeshTest {

	@Test
	public void testExternalEventbusMessage() throws Exception {

		String allowedAddress = MESH_MIGRATION;
		CountDownLatch latch = new CountDownLatch(1);
		client().eventbus(ws -> {
			// Register
			JsonObject msg = new JsonObject().put("type", "register").put("address", allowedAddress);
			ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

			// Handle msgs
			ws.handler(buff -> {
				String str = buff.toString();
				System.out.println(str);
				JsonObject received = new JsonObject(str);
				JsonObject rec = received.getJsonObject("body");
				String value = rec.getString("test");
				assertEquals("someValue", value);
				latch.countDown();
			});

		});

		Thread.sleep(1000);
		JsonObject msg = new JsonObject();
		msg.put("test", "someValue");
		Mesh.vertx().eventBus().send(allowedAddress, msg);

		failingLatch(latch);
	}

	@Test(timeout = 10_000)
	public void testNodeDeleteEvent(TestContext context) throws Exception {
		Async async = context.async();
		client().eventbus(ws -> {
			// Register
			JsonObject msg = new JsonObject().put("type", "register").put("address", EVENT_NODE_DELETED);
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
		});

		call(() -> client().deleteNode(PROJECT_NAME, contentUuid()));
	}

	@Test(timeout = 10_000)
	public void testNodeDeleteLanguageEvent(TestContext context) throws Exception {
		Async async = context.async();
		client().eventbus(ws -> {
			// Register
			JsonObject msg = new JsonObject().put("type", "register").put("address", EVENT_NODE_DELETED);
			ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

			// Handle msgs
			ws.handler(buff -> {
				String str = buff.toString();
				JsonObject received = new JsonObject(str);
				context.assertNotNull(received.getJsonObject("body").getString("uuid"));
				context.assertEquals("content", received.getJsonObject("body").getString("schemaName"));
				context.assertEquals("en", received.getJsonObject("body").getString("languageTag"));
				async.complete();
			});
		});

		call(() -> client().deleteNode(PROJECT_NAME, contentUuid(),"en"));
	}

	@Test
	public void testNodeUpdateEvent() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		client().eventbus(ws -> {
			// Register
			JsonObject msg = new JsonObject().put("type", "register").put("address", EVENT_NODE_UPDATED);
			ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

			// Handle msgs
			ws.handler(buff -> {
				String str = buff.toString();
				JsonObject received = new JsonObject(str);
				assertNotNull(received.getJsonObject("body").getString("uuid"));
				assertEquals("content", received.getJsonObject("body").getString("schemaName"));
				latch.countDown();
			});
		});

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		NodeUpdateRequest request = new NodeUpdateRequest();
		request.getFields().put("slug", FieldUtil.createStringField("blub"));
		request.setVersion(response.getVersion());
		request.setLanguage("en");
		call(() -> client().updateNode(PROJECT_NAME, contentUuid(), request));

		NodeResponse response2 = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		assertNotEquals(response.getVersion(), response2.getVersion());
		failingLatch(latch);
	}

	@Test
	public void testRegisterToEventbus() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		client().eventbus(ws -> {
			// Register
			JsonObject msg = new JsonObject().put("type", "register").put("address", "some-address");
			ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

			// Handle msgs
			ws.handler(buff -> {
				String str = buff.toString();
				JsonObject received = new JsonObject(str);
				Object rec = received.getValue("body");
				System.out.println("Handler:" + rec.toString());
				latch.countDown();
			});

			// Send msg
			msg = new JsonObject().put("type", "publish").put("address", "some-address").put("body", "someText");
			ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

		});
		failingLatch(latch);
	}

}
