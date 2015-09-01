package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.MeshVertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface SearchQueueEntry extends MeshVertex {

	String getElementUuid();

	SearchQueueEntry setElementUuid(String uuid);

	String getElementType();

	SearchQueueEntry setElementType(String type);

	SearchQueueEntryAction getElementAction();

	SearchQueueEntry setElementAction(String action);

	SearchQueueEntry process(Handler<AsyncResult<Void>> handler);

	String getElementActionName();

	SearchQueueEntry setElementIndexType(String indexType);

	String getElementIndexType();

}
