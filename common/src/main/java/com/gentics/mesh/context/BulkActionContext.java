package com.gentics.mesh.context;

import com.gentics.mesh.core.data.search.SearchQueueBatch;

public interface BulkActionContext {

	/**
	 * Increment the counter which tracks deleted elements.
	 * 
	 * @return
	 */
	long inc();

	/**
	 * Process the deletion by processing the batch and committing the transaction.
	 */
	void process();

	/**
	 * Process the actions by processing the batch and committing the transaction. The method will also automatically increase the counter.
	 * 
	 * @param force
	 *            Force the commit / process even if the batch is not yet full
	 */
	void process(boolean force);

	/**
	 * Return the batch of this context.
	 * 
	 * @return
	 */
	SearchQueueBatch batch();

	/**
	 * Add a drop index entry to the batch.
	 * 
	 * @param composeIndexName
	 */
	void dropIndex(String composeIndexName);

}
