package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.MeshVertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface SearchQueueEntry extends MeshVertex {

	/**
	 * Return the search queue entry element uuid which identifies the element that should be handled.
	 * 
	 * @return
	 */
	String getElementUuid();

	SearchQueueEntry setElementUuid(String uuid);

	String getElementType();

	SearchQueueEntry setElementType(String type);

	SearchQueueEntryAction getElementAction();

	SearchQueueEntry setElementAction(String action);

	SearchQueueEntry process(Handler<AsyncResult<Void>> handler);

	/**
	 * Return the search queue action name.
	 * 
	 * @return
	 */
	String getElementActionName();

	SearchQueueEntry setElementIndexType(String indexType);

	String getElementIndexType();

}
