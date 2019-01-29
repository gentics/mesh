package com.gentics.mesh.core.data.search;

import com.gentics.mesh.context.BulkActionContext;

/**
 * The search queue maintains the search queue batch related actions. Each batch is used to update the search index documents.
 */
public interface SearchQueue {

	/**
	 * Create a new search queue batch.
	 * 
	 * @return Created batch
	 */
	EventQueueBatch create();

	/**
	 * Create a new bulk action context which holds a fresh search queue batch.
	 * 
	 * @return Created context
	 */
	BulkActionContext createBulkContext();

}
