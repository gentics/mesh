package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.MeshVertex;

public interface SearchQueue extends MeshVertex {

	public static final String SEARCH_QUEUE_ENTRY_ADDRESS = "search-queue-entry";

	/**
	 * Add a search queue batch to the queue.
	 * 
	 * @param entry
	 */
	void addBatch(SearchQueueBatch batch);

	/**
	 * Fetch a search queue batch and remove it from the queue.
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	SearchQueueBatch take() throws InterruptedException;

	/**
	 * Returns the size of the queue.
	 * 
	 * @return
	 */
	long getSize();

	SearchQueueBatch createBatch();

}
