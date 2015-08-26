package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;

import java.util.concurrent.locks.ReentrantLock;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;

public class SearchQueueImpl extends MeshVertexImpl implements SearchQueue {

	/** Lock held by take, poll, etc */
	private final ReentrantLock takeLock = new ReentrantLock();

	@Override
	public void put(SearchQueueEntry entry) {
		setLinkOutTo(entry.getImpl(), HAS_ITEM);
	}

	@Override
	public void put(String uuid, String type, SearchQueueEntryAction action) {
		SearchQueueEntry entry = getGraph().addFramedVertex(SearchQueueEntryImpl.class);
		entry.setElementUuid(uuid);
		entry.setElementType(type);
		entry.setAction(action.getName());
		put(entry);
	}

	@Override
	public void put(GenericVertex<?> vertex, SearchQueueEntryAction action) {
		put(vertex.getUuid(), vertex.getType(), action);
	}

	@Override
	public SearchQueueEntry take() throws InterruptedException {
		SearchQueueEntry entry;
		final ReentrantLock takeLock = this.takeLock;
		takeLock.lockInterruptibly();
		try {
			entry = out(HAS_ITEM).nextOrDefault(SearchQueueEntryImpl.class, null);
			if (entry != null) {
				unlinkOut(entry.getImpl(), HAS_ITEM);
				return entry;
			} else {
				return null;
			}

		} finally {
			takeLock.unlock();
		}

	}

	@Override
	public long getSize() {
		return out(HAS_ITEM).count();
	}

}
