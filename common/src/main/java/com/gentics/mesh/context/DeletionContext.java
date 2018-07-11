package com.gentics.mesh.context;

import java.util.concurrent.atomic.AtomicLong;

import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Context which tracks the active deletion operation.
 */
public class DeletionContext {

	private static final Logger log = LoggerFactory.getLogger(DeletionContext.class);

	private static final int DEFAULT_BATCH_SIZE = 100;

	private final AtomicLong counter = new AtomicLong(0);

	private SearchQueueBatch batch;

	public DeletionContext(SearchQueueBatch batch) {
		this.batch = batch;
	}

	/**
	 * Increment the counter which tracks deleted elements.
	 * 
	 * @return
	 */
	public long inc() {
		return counter.incrementAndGet();
	}

	/**
	 * Process the deletion by processing the batch and committing the transaction.
	 */
	public void process() {
		process(false);
	}

	/**
	 * Process the deletion by processing the batch and committing the transaction. The method will also automatically increase the counter.
	 * 
	 * @param force
	 *            Force the commit / process even if the batch is not yet full
	 */
	public void process(boolean force) {
		if (counter.incrementAndGet() >= DEFAULT_BATCH_SIZE || force) {
			log.info("Processing transaction. I counted {" + counter.get() + "} elements.");
			Tx.getActive().getGraph().commit();
			batch.processSync();
			// Reset the counter back to zero
			counter.set(0);
		}
	}

	public void dropIndex(String composeIndexName) {
		batch.dropIndex(composeIndexName);
	}

	public SearchQueueBatch batch() {
		return batch;
	}

}
