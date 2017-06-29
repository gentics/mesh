package com.gentics.mesh.core.data.search.impl;

import java.util.concurrent.ArrayBlockingQueue;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;

/**
 * @see SearchQueue
 */
@Singleton
public class SearchQueueImpl extends ArrayBlockingQueue<SearchQueueBatch> implements SearchQueue {

	private static final long serialVersionUID = 8712625810525574681L;

	private final Object objectLock = new Object();

	@Inject
	public Provider<SearchQueueBatch> batchProvider;

	@Inject
	public SearchQueueImpl(Provider<SearchQueueBatch> provider) {
		super(MAX_QUEUE_SIZE);
		this.batchProvider = provider;
	}

	@Override
	public SearchQueueBatch create() {
		SearchQueueBatch batch = batchProvider.get();
		try {
			put(batch);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return batch;
	}

	@Override
	public void put(SearchQueueBatch e) throws InterruptedException {
		super.put(e);
	}

	@Override
	public boolean remove(SearchQueueBatch o) {
		boolean result = super.remove(o);
		if (isEmpty()) {
			synchronized (objectLock) {
				objectLock.notify();
			}
		}
		return result;
	}

	@Override
	public SearchQueue blockUntilEmpty(int timeoutInSeconds) throws InterruptedException {
		if (!isEmpty()) {
			synchronized (objectLock) {
				objectLock.wait(timeoutInSeconds * 1000);
			}
		}
		return this;
	}

}
