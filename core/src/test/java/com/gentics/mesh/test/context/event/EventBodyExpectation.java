package com.gentics.mesh.test.context.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.json.JsonUtil;

import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonObject;

public class EventBodyExpectation implements EventExpectation {

	private MeshEvent event;

	private int count;

	private Consumer<JsonObject> asserter;

	/**
	 * Create a new body expectation.
	 * 
	 * @param event
	 *            Event to inspect
	 * @param count
	 *            How many events should be passed by the tester
	 * @param clazzOfEM
	 *            Event body model class
	 * @param tester
	 *            Tester for the event model object
	 */
	public <EM extends MeshEventModel> EventBodyExpectation(MeshEvent event, int count, Class<EM> clazzOfEM, Consumer<EM> tester) {
		this.event = event;
		this.count = count;
		// Add the asserter
		this.asserter = (JsonObject e) -> {
			EM model = JsonUtil.readValue(e.toString(), clazzOfEM);
			tester.accept(model);
		};
	}

	@Override
	public void verify(Map<MeshEvent, List<JsonObject>> events) {
		List<JsonObject> list = events.get(event);
		assertNotNull("No events for type {" + event.getAddress() + "} have been recorded.", list);
		int accepts = 0;
		Throwable lastError = null;
		for (JsonObject json : list) {
			try {
				asserter.accept(json);
				accepts++;
			} catch (Throwable t) {
				lastError = t;
			}
		}
		// Asserter passed the expected amount of events
		if (accepts == count) {
			return;
		}
		if (lastError == null) {
			assertEquals("The body assertion for type {" + event.getAddress() + "} did not match for the expected amount of events.", count, accepts);
		} else {
			if (lastError instanceof AssertionError) {
				throw (AssertionError) lastError;
			} else {
				throw new AssertionError(
					"The body assertion for type {" + event.getAddress() + "} did not match for the expected amount of events. Passed: " + accepts
						+ " of expected: " + count,
					lastError);
			}
		}
	}

}
