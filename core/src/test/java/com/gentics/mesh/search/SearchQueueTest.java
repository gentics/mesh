package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_ACTION;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.graphdb.Trx;

public class SearchQueueTest extends AbstractBasicDBTest {

	@Test
	public void testQueue() throws InterruptedException, JSONException {
		SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
		SearchQueueBatch batch = searchQueue.createBatch("0");
		int i = 0;
		for (Node node : boot.nodeRoot().findAll()) {
			batch.addEntry(node.getUuid(), Node.TYPE, CREATE_ACTION);
			i++;
		}
		long size = searchQueue.getSize();
		SearchQueueBatch loadedBatch = searchQueue.take();
		assertNotNull(loadedBatch);

		assertEquals(i, loadedBatch.getEntries().size());
		SearchQueueEntry entry = loadedBatch.getEntries().get(0);
		assertNotNull(entry);
		assertEquals(size - 1, searchQueue.getSize());
		size = searchQueue.getSize();
		for (int e = 0; e < size; e++) {
			batch = searchQueue.take();
			assertNotNull("Batch " + e + " was null.", batch);
		}
		assertEquals("We took all elements. The queue should be empty", 0, searchQueue.getSize());
		batch = searchQueue.take();
		assertNull(batch);

	}

	@Test
	public void testQueueThreadSafety() throws Exception {

		// Add some entries to the search queue
		SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
		SearchQueueBatch batch = searchQueue.createBatch("0");
		for (Node node : boot.nodeRoot().findAll()) {
			batch.addEntry(node.getUuid(), Node.TYPE, CREATE_ACTION);
		}

		long size = searchQueue.getSize();
		assertNotNull(batch);
		assertEquals(size - 1, searchQueue.getSize());

		size = searchQueue.getSize();
		System.out.println("Size: " + size);
		CountDownLatch latch = new CountDownLatch((int) size);
		for (int i = 0; i <= size; i++) {
			Runnable r = () -> {
				// int z = 0;
				while (true) {
					// try {
					try (Trx txTake = db.trx()) {
						try {
							SearchQueueBatch currentBatch = searchQueue.take();
							assertNotNull("Batch was null.", currentBatch);
						} catch (Exception e) {
							fail(e.getMessage());
						}
						txTake.success();
					}
					System.out.println("Got the element");
					latch.countDown();
					break;
					// } catch (OConcurrentModificationException e) {
					// System.out.println("Got it - Try: " + z + " Size: " + searchQueue.getSize());
					// z++;
					// }
				}
			};
			Thread t = new Thread(r);
			t.start();
			failingLatch(latch);
		}

		searchQueue.reload();
		assertEquals("We took all elements. The queue should be empty", 0, searchQueue.getSize());
		batch = searchQueue.take();
		assertNull(batch);

		CountDownLatch latch2 = new CountDownLatch(10);
		AtomicReference<AssertionError> errorReference = new AtomicReference<>();
		for (int i = 0; i < 10; i++) {
			Runnable r = () -> {
				try (Trx tx2 = db.trx()) {
					try {
						SearchQueueBatch currentBatch = searchQueue.take();
						latch2.countDown();
						try {
							assertNull("Batch was null.", currentBatch);
						} catch (AssertionError e) {
							if (errorReference.get() == null) {
								errorReference.set(e);
							}
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			Thread t = new Thread(r);
			t.start();
		}
		failingLatch(latch2);
		if (errorReference.get() != null) {
			throw errorReference.get();
		}

	}
}
