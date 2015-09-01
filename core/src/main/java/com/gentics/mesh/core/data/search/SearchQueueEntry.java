package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.MeshVertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface SearchQueueEntry extends MeshVertex {

	String getElementUuid();

	SearchQueueEntry setElementUuid(String uuid);

	String getElementType();

	SearchQueueEntry setElementType(String type);

	SearchQueueEntryAction getElementAction();

	SearchQueueEntry setElementAction(String action);

	JsonObject getMessage();

	void process(Handler<AsyncResult<Void>> handler);

	String getElementActionName();

}
