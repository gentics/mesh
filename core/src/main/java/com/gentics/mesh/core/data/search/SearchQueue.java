package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.MeshVertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public interface SearchQueue extends MeshVertex {

	/**
	 * Fetch a search queue batch and remove it from the queue.
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	SearchQueueBatch take() throws InterruptedException;

	/**
	 * Fetch the search queue batch with the given id and remove it from the queue.
	 * 
	 * @param batchId
	 * @return
	 */
	SearchQueueBatch take(String batchId);

	/**
	 * Returns the size of the queue.
	 * 
	 * @return
	 */
	long getSize();

	/**
	 * Create a new batch with the given id and add it to the queue.
	 * 
	 * @param batchId
	 * @return
	 */
	SearchQueueBatch createBatch(String batchId);

	/**
	 * Add all objects within the graph via a single batch to the search queue.
	 */
	void addFullIndex();

	/**
	 * Process all search queue batches and invoke the handler when an error occurred or the batches were processed correctly.
	 * 
	 * @param handler
	 * @throws InterruptedException
	 */
	void processAll(Handler<AsyncResult<Future<Void>>> handler) throws InterruptedException;

	void remove(SearchQueueBatch batch);

	void add(SearchQueueBatch batch);

}
