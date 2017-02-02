package com.gentics.mesh.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.test.AbstractDBTest;

public class SearchQueueBatchTest extends AbstractDBTest {

	@Test
	public void testQueueLimit() throws InterruptedException {
		SearchQueue queue = MeshInternal.get().searchQueue();

		final Set<SearchQueueBatch> batches = new HashSet<>();

		Thread t = new Thread(() -> {
			for (int i = 0; i < SearchQueue.MAX_QUEUE_SIZE + 1; i++) {
				batches.add(queue.create());
			}
		});
		t.start();
		Thread.sleep(1000);
		assertTrue("The queue should block and thus the thread should be still alive.", t.isAlive());

		batches.iterator().next().processSync();
		Thread.sleep(1000);
		assertFalse("The queue lock should have been released and the thread should have been terminated by now", t.isAlive());

		Thread t2 = new Thread(() -> {
			for (SearchQueueBatch batch : batches) {
				batch.processSync();
			}
		});

		t2.start();
		queue.blockUntilEmpty(20);
		assertTrue("The queue should be empty by now.", queue.isEmpty());

	}
}
