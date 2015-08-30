package com.gentics.mesh.core.data.search;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import org.elasticsearch.action.ActionResponse;

import com.gentics.mesh.core.data.MeshVertex;

public interface SearchQueueEntry extends MeshVertex {

	String getElementUuid();

	SearchQueueEntry setElementUuid(String uuid);

	String getElementType();

	SearchQueueEntry setElementType(String type);

	SearchQueueEntryAction getElementAction();

	SearchQueueEntry setElementAction(String action);

	JsonObject getMessage();

	void process(Handler<AsyncResult<ActionResponse>> handler);

	String getElementActionName();

}
