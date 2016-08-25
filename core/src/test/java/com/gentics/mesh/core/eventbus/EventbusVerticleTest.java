package com.gentics.mesh.core.eventbus;

import static com.gentics.mesh.core.verticle.eventbus.EventbusAddress.MESH_MIGRATION;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class EventbusVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.eventbusVerticle());
		return list;
	}

	@Test
	public void testExternalEventbusMessage() throws Exception {

		String allowedAddress = MESH_MIGRATION.toString();
		CountDownLatch latch = new CountDownLatch(1);
		getClient().eventbus(ws -> {
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

	@Test
	public void testRegisterToEventbus() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		getClient().eventbus(ws -> {
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
