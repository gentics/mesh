package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_ACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.jettison.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractDBTest;

public class SearchQueueTest extends AbstractDBTest {

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testQueue() throws InterruptedException, JSONException {

		SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
		for (Node node : boot.nodeRoot().findAll()) {
			searchQueue.put(node.getUuid(), Node.TYPE, CREATE_ACTION);
		}
		long size = searchQueue.getSize();
		SearchQueueEntry entry = searchQueue.take();
		assertNotNull(entry);
		assertEquals(size - 1, searchQueue.getSize());
		size = searchQueue.getSize();
		for (int i = 0; i < size; i++) {
			entry = searchQueue.take();
			assertNotNull("entry " + i + " was null." + entry);
		}
		assertEquals("We took all elements. The queue should be empty", 0, searchQueue.getSize());
		entry = searchQueue.take();
		assertNull(entry);

	}

	@Test
	public void testQueueThreadSafety() throws InterruptedException {
		try (Trx tx = new Trx(db)) {
			SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
			for (Node node : boot.nodeRoot().findAll()) {
				searchQueue.put(node.getUuid(), Node.TYPE, CREATE_ACTION);
			}
			long size = searchQueue.getSize();
			SearchQueueEntry entry = searchQueue.take();
			assertNotNull(entry);
			assertEquals(size - 1, searchQueue.getSize());

			size = searchQueue.getSize();
			CountDownLatch latch = new CountDownLatch((int) size);
			for (int i = 0; i < size; i++) {
				Runnable r = () -> {
					try {
						SearchQueueEntry currentEntry = searchQueue.take();
						latch.countDown();
						assertNotNull("entry was null." + currentEntry);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				};
				Thread t = new Thread(r);
				t.start();
			}
			latch.await();
			assertEquals("We took all elements. The queue should be empty", 0, searchQueue.getSize());
			entry = searchQueue.take();
			assertNull(entry);

			CountDownLatch latch2 = new CountDownLatch(10);
			AtomicReference<AssertionError> errorReference = new AtomicReference<>();
			for (int i = 0; i < 10; i++) {
				Runnable r = () -> {
					try {
						SearchQueueEntry currentEntry = searchQueue.take();
						latch2.countDown();
						try {
							assertNull("entry was not null.", currentEntry);
						} catch (AssertionError e) {
							if (errorReference.get() == null) {
								errorReference.set(e);
							}
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				};
				Thread t = new Thread(r);
				t.start();
			}
			latch2.await();
			if (errorReference.get() != null) {
				throw errorReference.get();
			}
		}

	}
}
