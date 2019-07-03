package com.gentics.mesh.search.verticle.bulk;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.gentics.mesh.core.data.search.request.Bulkable;

/**
 * A queue that holds bulkable search requests and counts the total length of the items.
 */
class BulkQueue {
	// TODO Use SpscArrayQueue https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0#queues
	private final Queue<Bulkable> bulkableRequests = new ConcurrentLinkedQueue<>();
	private long bulkLength = 0;

	public boolean add(Bulkable bulkable) {
		bulkLength += bulkable.bulkLength();
		return bulkableRequests.add(bulkable);
	}

	public int size() {
		return bulkableRequests.size();
	}

	public boolean isEmpty() {
		return bulkableRequests.isEmpty();
	}

	public long getBulkLength() {
		return bulkLength;
	}

	public void clear() {
		bulkableRequests.clear();
		bulkLength = 0;
	}

	public List<Bulkable> asList() {
		return new ArrayList<>(bulkableRequests);
	}
}
