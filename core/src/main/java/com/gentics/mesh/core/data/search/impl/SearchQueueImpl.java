package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BATCH;

import static com.gentics.mesh.core.data.search.SearchQueueBatch.BATCH_ID_PROPERTY_KEY;

import java.util.concurrent.locks.ReentrantLock;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;

public class SearchQueueImpl extends MeshVertexImpl implements SearchQueue {

	/** Lock held by take, poll, etc */
	private final ReentrantLock takeLock = new ReentrantLock();

	@Override
	public void addBatch(SearchQueueBatch batch) {
		setLinkOutTo(batch.getImpl(), HAS_BATCH);
	}

	@Override
	public SearchQueueBatch take() throws InterruptedException {
		SearchQueueBatch entry;
		final ReentrantLock takeLock = this.takeLock;
		takeLock.lockInterruptibly();
		try {
			entry = out(HAS_BATCH).nextOrDefault(SearchQueueBatchImpl.class, null);
			if (entry != null) {
				unlinkOut(entry.getImpl(), HAS_BATCH);
				return entry;
			} else {
				return null;
			}

		} finally {
			takeLock.unlock();
		}

	}

	@Override
	public SearchQueueBatch take(String batchId) {
		SearchQueueBatch entry;
		final ReentrantLock takeLock = this.takeLock;
		try {
			takeLock.lockInterruptibly();
			try {
				entry = out(HAS_BATCH).has(BATCH_ID_PROPERTY_KEY).nextOrDefault(SearchQueueBatchImpl.class, null);
				if (entry != null) {
					unlinkOut(entry.getImpl(), HAS_BATCH);
					return entry;
				} else {
					return null;
				}
			} finally {
				takeLock.unlock();
			}
		} catch (InterruptedException e) {
			//TODO handle  InterruptedException 
			e.printStackTrace();
		}
		return null;

	}

	@Override
	public SearchQueueBatch createBatch(String batchId) {
		SearchQueueBatch batch = getGraph().addFramedVertex(SearchQueueBatchImpl.class);
		batch.setBatchId(batchId);
		addBatch(batch);
		return batch;
	}

	@Override
	public long getSize() {
		return out(HAS_BATCH).count();
	}

}
