package com.gentics.mesh.context.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.event.EventQueueBatch;

import io.reactivex.Completable;

/**
 * Context which tracks recursive and bulk actions.
 * 
 * Some operations may affect a lot of elements and thus it is needed to commit the transaction at specific safe points in order to reduce the memory footprint.
 */
public class BulkActionContextImpl implements BulkActionContext {

	private static final Logger log = LoggerFactory.getLogger(BulkActionContextImpl.class);

	public static final int DEFAULT_BATCH_SIZE = 100;

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
	public void setBatch(EventQueueBatch batch) {
		batch.addAll(this.batch);
		this.batch = batch;
	}

	@Override
	public void process(boolean force) {
		if (elementCounter.incrementAndGet() >= DEFAULT_BATCH_SIZE || force) {
			log.info("Processing transaction batch {" + batchCounter.get() + "}. I counted {" + elementCounter.get() + "} elements. {" + asyncActions.size() + "} to be executed.");
			Tx.maybeGet().ifPresent(Tx::commit);
			Completable.merge(asyncActions).subscribe(() -> {
				log.trace("Async action processed");
			});
			batch().dispatch();
			// Reset the context
			asyncActions.clear();
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
