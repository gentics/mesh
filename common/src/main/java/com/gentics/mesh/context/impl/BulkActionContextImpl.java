package com.gentics.mesh.context.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Provider;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Completable;
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

	private List<Completable> asyncActions = new ArrayList<>();
	private EventQueueBatch batch;
	private Database db;

	@Inject
	public BulkActionContextImpl(Provider<EventQueueBatch> provider, Database db) {
		this.batch = provider.get();
		this.db = db;
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
			// Check before commit to ensure we are 100% safe
			db.blockingTopologyLockCheck();
			Tx.get().commit();
			Completable.merge(asyncActions).subscribe(() -> {
				log.trace("Async action processed");
			});
			batch().dispatch();
			// Reset the counter back to zero
			elementCounter.set(0);
			batchCounter.incrementAndGet();
		}
	}

	@Override
	public EventQueueBatch batch() {
		return batch;
	}

	@Override
	public void add(Completable action) {
		asyncActions.add(action);
	}

}
