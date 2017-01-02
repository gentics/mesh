package com.gentics.mesh.core.data.search;

import java.util.Queue;

/**
 * A search queue is a queue which holds search queue batches. Each batch is used to update the search index documents. Once a batch has been processed it
 * should be removed from the search queue.
 */
public interface SearchQueue extends Queue<SearchQueueBatch> {

	/**
	 * Process all search queue batches.
	 * 
	 * @throws InterruptedException
	 * @return Amount of batches that have been processed
	 */
	long processAll() throws InterruptedException;

	/**
	 * Create a new search queue batch and add it to the queue.
	 * 
	 * @return Created batch
	 */
	SearchQueueBatch createBatch();

	/**
	 * Add all objects within the graph via a single batch to the search queue.
	 * 
	 * @return Fluent API
	 */
	SearchQueue addFullIndex();

}
