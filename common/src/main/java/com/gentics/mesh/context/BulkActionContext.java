package com.gentics.mesh.context;

import java.util.concurrent.atomic.AtomicLong;

import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.madl.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Context which tracks recursive and bulk actions.
 * 
 * Some operations may affect a lot of elements and thus it is needed to commit the transaction at specific safe points in order to reduce the memory footprint.
 */
public class BulkActionContext {

	private static final Logger log = LoggerFactory.getLogger(BulkActionContext.class);

	private static final int DEFAULT_BATCH_SIZE = 100;

	private final AtomicLong batchCounter = new AtomicLong(1);
	private final AtomicLong elementCounter = new AtomicLong(0);

	private SearchQueueBatch batch;

	public BulkActionContext(SearchQueueBatch batch) {
		this.batch = batch;
	}

	/**
	 * Increment the counter which tracks deleted elements.
	 * 
	 * @return
	 */
	public long inc() {
		return elementCounter.incrementAndGet();
	}

	/**
	 * Process the deletion by processing the batch and committing the transaction.
	 */
	public void process() {
		process(false);
	}

	/**
	 * Process the actions by processing the batch and committing the transaction. The method will also automatically increase the counter.
	 * 
	 * @param force
	 *            Force the commit / process even if the batch is not yet full
	 */
	public void process(boolean force) {
		if (elementCounter.incrementAndGet() >= DEFAULT_BATCH_SIZE || force) {
			log.info("Processing transaction batch {" + batchCounter.get() + "}. I counted {" + elementCounter.get() + "} elements.");
			batch.processSync();
			Tx.get().commit();
			// Reset the counter back to zero
			elementCounter.set(0);
			batchCounter.incrementAndGet();
		}
	}

	public void dropIndex(String composeIndexName) {
		batch.dropIndex(composeIndexName);
	}

	public SearchQueueBatch batch() {
		return batch;
	}

}
