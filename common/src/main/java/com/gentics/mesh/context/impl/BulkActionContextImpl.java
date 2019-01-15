package com.gentics.mesh.context.impl;

import java.util.concurrent.atomic.AtomicLong;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Context which tracks recursive and bulk actions.
 * 
 * Some operations may affect a lot of elements and thus it is needed to commit the transaction at specific safe points in order to reduce the memory footprint.
 */
public class BulkActionContextImpl implements BulkActionContext {

	private static final Logger log = LoggerFactory.getLogger(BulkActionContextImpl.class);

	private static final int DEFAULT_BATCH_SIZE = 100;

	private final AtomicLong batchCounter = new AtomicLong(1);
	private final AtomicLong elementCounter = new AtomicLong(0);

	private SearchQueueBatch batch;

	public BulkActionContextImpl(SearchQueueBatch batch) {
		this.batch = batch;
	}

	@Override
	public long inc() {
		return elementCounter.incrementAndGet();
	}

	@Override
	public void process() {
		process(false);
	}

	@Override
	public void process(boolean force) {
		if (elementCounter.incrementAndGet() >= DEFAULT_BATCH_SIZE || force) {
			log.info("Processing transaction batch {" + batchCounter.get() + "}. I counted {" + elementCounter.get() + "} elements.");
			batch.processSync();
			Tx.getActive().getGraph().commit();
			// Reset the counter back to zero
			elementCounter.set(0);
			batchCounter.incrementAndGet();
		}
	}

	@Override
	public void dropIndex(String composeIndexName) {
		batch.dropIndex(composeIndexName);
	}

	@Override
	public SearchQueueBatch batch() {
		return batch;
	}

}
