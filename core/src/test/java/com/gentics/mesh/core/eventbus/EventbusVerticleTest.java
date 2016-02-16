package com.gentics.mesh.core.eventbus;

import static com.gentics.mesh.util.MeshAssert.failingLatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.json.JsonObject;

public class EventbusVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private EventbusVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
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
