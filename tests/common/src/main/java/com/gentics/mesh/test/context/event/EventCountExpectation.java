package com.gentics.mesh.test.context.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.MeshEvent;

import io.vertx.core.json.JsonObject;

public class EventCountExpectation implements EventExpectation {

	private MeshEvent event;
	private long count;

	public EventCountExpectation(MeshEvent event, long expectedCount) {
		this.event = event;
		this.count = expectedCount;
	}

	@Override
	public void verify(Map<MeshEvent, List<JsonObject>> events) {
		List<JsonObject> list = events.get(event);
		assertNotNull("No events for type {" + event.getAddress() + "} were received.", list);
		assertEquals("The incorrect expected count of events for type {" + event.getAddress() + "} was received.", count, list.size());
	}

}
