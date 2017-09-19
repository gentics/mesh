package com.gentics.mesh.core.data.search;

/**
 * The search queue maintains the search queue batch related actions. Each batch is used to update the search index documents.
 */
public interface SearchQueue {

	/**
	 * Create a new search queue batch.
	 * 
	 * @return Created batch
	 */
	SearchQueueBatch create();

}
