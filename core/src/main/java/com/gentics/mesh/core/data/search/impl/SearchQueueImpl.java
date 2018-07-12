package com.gentics.mesh.core.data.search.impl;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;

/**
 * @see SearchQueue
 */
@Singleton
public class SearchQueueImpl implements SearchQueue {

	@Inject
	public Provider<SearchQueueBatch> batchProvider;

	@Inject
	public SearchQueueImpl(Provider<SearchQueueBatch> provider) {
		this.batchProvider = provider;
	}

	@Override
	public SearchQueueBatch create() {
		SearchQueueBatch batch = batchProvider.get();
		return batch;
	}

	@Override
	public BulkActionContext createBulkContext() {
		return new BulkActionContext(create());
	}

}
