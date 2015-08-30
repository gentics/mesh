package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.MeshVertex;

public interface SearchQueue extends MeshVertex {

//	public static final String SEARCH_QUEUE_ENTRY_ADDRESS = "search-queue-entry";

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

	void addFullIndex();

	void processAll();

}
