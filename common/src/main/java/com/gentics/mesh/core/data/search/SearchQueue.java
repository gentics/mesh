package com.gentics.mesh.core.data.search;

/**
 * The search queue maintains the search queue batch related actions. Each batch is used to update the search index documents.
 */
//TODO rename class because it is no longer a queue
public interface SearchQueue {

	/**
	 * Create a new search queue batch and add it to the queue.
	 * 
	 * @return Created batch
	 */
	SearchQueueBatch createBatch();

	/**
	 * Add all objects within the graph via a single batch.
	 * 
	 * @return Fluent API
	 */
	SearchQueueBatch addFullIndex();

}
