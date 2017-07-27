package com.gentics.mesh.core.data.search;

import java.util.concurrent.BlockingQueue;

/**
 * The search queue maintains the search queue batch related actions. Each batch is used to update the search index documents.
 */
public interface SearchQueue extends BlockingQueue<SearchQueueBatch> {

	public final static int MAX_QUEUE_SIZE = 5000;

	/**
	 * Create a new search queue batch and add it to the queue.
	 * 
	 * @return Created batch
	 */
	SearchQueueBatch create();

	/**
	 * Block until the search queue has completed.
	 * 
	 * @param timeoutInSeconds
	 * @return Fluent API
	 * @throws InterruptedException
	 */
	SearchQueue blockUntilEmpty(int timeoutInSeconds) throws InterruptedException;

	/**
	 * Remove the batch from the queue.
	 */
	boolean remove(SearchQueueBatch batch);

}
