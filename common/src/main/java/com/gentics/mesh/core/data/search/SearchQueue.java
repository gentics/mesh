package com.gentics.mesh.core.data.search;

import com.gentics.mesh.context.DeletionContext;

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

	/**
	 * Create a new deletion context which holds a fresh search queue batch.
	 * 
	 * @return Created context
	 */
	DeletionContext createDeletionContext();

}
