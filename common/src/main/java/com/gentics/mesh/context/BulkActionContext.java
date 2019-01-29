package com.gentics.mesh.context;

import com.gentics.mesh.context.impl.BulkActionContextImpl;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.event.EventQueueBatch;

public interface BulkActionContext {

	/**
	 * Create a new context.
	 * 
	 * @return
	 */
	static BulkActionContext create() {
		return new BulkActionContextImpl();
	}

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
	EventQueueBatch batch();

//	/**
//	 * Add a drop index entry to the batch.
//	 * 
//	 * @param composeIndexName
//	 */
//	void dropIndex(String composeIndexName);

	void delete(IndexableElement element, boolean b);

}
