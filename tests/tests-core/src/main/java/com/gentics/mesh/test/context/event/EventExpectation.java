package com.gentics.mesh.test.context.event;

import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.MeshEvent;

import io.vertx.core.json.JsonObject;

/**
 * Expectation which will be asserted against the received events.
 */
public interface EventExpectation {

	/**
	 * Verify that the expectation matches the found events.
	 * 
	 * @param events
	 */
	void verify(Map<MeshEvent, List<JsonObject>> events);

}
