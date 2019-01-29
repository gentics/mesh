package com.gentics.mesh.core.data.search.impl;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.context.impl.BulkActionContextImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.EventQueueBatch;

/**
 * @see SearchQueue
 */
@Singleton
public class SearchQueueImpl implements SearchQueue {

	@Inject
	public Provider<EventQueueBatch> batchProvider;

	@Inject
	public SearchQueueImpl(Provider<EventQueueBatch> provider) {
		this.batchProvider = provider;
	}

	@Override
	public EventQueueBatch create() {
		EventQueueBatch batch = batchProvider.get();
		return batch;
	}

	@Override
	public BulkActionContextImpl createBulkContext() {
		return new BulkActionContextImpl(create());
	}

}
