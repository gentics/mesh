package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.request.ElasticsearchRequest;

import java.util.List;

public interface EventTransformer {
	/**
	 * Handles an event from mesh. Creates elasticsearch document from the data of the graph and creates a list
	 * of requests that represent the data that was changed during the event.
	 *
	 * @param messageEvent
	 * @return
	 */
	List<ElasticsearchRequest> handle(MessageEvent messageEvent);
}
