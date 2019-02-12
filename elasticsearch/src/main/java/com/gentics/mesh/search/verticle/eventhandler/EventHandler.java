package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.rest.MeshEvent;

import java.util.Collection;

public interface EventHandler extends EventTransformer {
	/**
	 * Gets collection of all events that can be handled by this class.
	 * @return
	 */
	Collection<MeshEvent> handledEvents();
}
