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

	/**
	 * Return the search queue entry action (eg. Update, delete..)
	 * 
	 * @return
	 */
	SearchQueueEntryAction getElementAction();

	/**
	 * Set the entry action (eg. update, delete, create)
	 * 
	 * @param action
	 * @return
	 */
	SearchQueueEntry setElementAction(String action);

	/**
	 * Process the entry and invoke the handler once processing failed or completed successfully.
	 * 
	 * @param handler
	 * @return
	 */
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
