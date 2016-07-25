package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.MeshVertex;

/**
 * A search queue is a queue which holds search queue batches. Each batch is used to update the search index documents. Once a batch has been processed it
 * should be removed from the search queue.
 */
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
	 * Process all search queue batches.
	 * 
	 * @throws InterruptedException
	 * @return Amount of batches that have been processed
	 */
	long processAll() throws InterruptedException;

	/**
	 * Remove the search queue batch from the queue.
	 * 
	 * @param batch
	 */
	void remove(SearchQueueBatch batch);

	/**
	 * Add the search queue batch to the queue.
	 * 
	 * @param batch
	 */
	void add(SearchQueueBatch batch);

	/**
	 * Clear the search queue and remove all batches and the connected entries.
	 */
	void clear();

}
