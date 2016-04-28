package com.gentics.mesh.util;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.node.ElementEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.TestUtils;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class MeshAssert {

	private static final Logger log = LoggerFactory.getLogger(MeshAssert.class);

	private static final Integer CI_TIMEOUT_SECONDS = 60;

	private static final Integer DEV_TIMEOUT_SECONDS = 10000;

	public static void assertSuccess(Future<?> future) {
		if (future.cause() != null) {
			future.cause().printStackTrace();
		}
		assertTrue("The future failed with error {" + (future.cause() == null ? "Unknown error" : future.cause().getMessage()) + "}",
				future.succeeded());
	}

	public static void assertElement(RootVertex<?> root, String uuid, boolean exists) throws Exception {
		root.reload();
		Object element = root.findByUuid(uuid).toBlocking().first();
		if (exists) {
			assertNotNull("The element should exist.", element);
		} else {
			assertNull("The element should not exist.", element);
		}

	}

	public static int getTimeout() throws UnknownHostException {
		int timeout = CI_TIMEOUT_SECONDS;
		if (TestUtils.isHost("plexus") || TestUtils.isHost("satan3.office")) {
			timeout = DEV_TIMEOUT_SECONDS;
		}
		if (log.isDebugEnabled()) {
			log.debug("Using test timeout of {" + timeout + "} seconds for host {" + TestUtils.getHostname() + "}");
		}
		return timeout;
	}

	public static void latchFor(Future<?> future) {
		CountDownLatch latch = new CountDownLatch(1);
		future.setHandler(rh -> {
			latch.countDown();
		});
		try {
			assertTrue("The timeout of the latch was reached.", latch.await(getTimeout(), TimeUnit.SECONDS));
		} catch (UnknownHostException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void failingLatch(CountDownLatch latch, int timeoutInSeconds) throws InterruptedException {
		if (!latch.await(timeoutInSeconds, TimeUnit.SECONDS)) {
			fail("Latch timeout reached");
		}
	}

	public static void failingLatch(CountDownLatch latch) throws Exception {
		if (!latch.await(getTimeout(), TimeUnit.SECONDS)) {
			fail("Latch timeout reached");
		}
	}

	/**
	 * Validate the list of affected elements by checking whether they were removed or not and also check whether the provided search queue batch contains the
	 * expected entries.
	 * 
	 * @param affectedElements
	 * @param batch
	 */
	public static void assertAffectedElements(Map<String, ElementEntry> affectedElements, SearchQueueBatch batch) {
		long nExpectedBatchEntries = 0;
		for (String key : affectedElements.keySet()) {
			ElementEntry entry = affectedElements.get(key);
			// 1. Check for deletion from graph
			if (DELETE_ACTION.equals(entry.getAction())) {
				assertFalse("The element {" + key + "} vertex for uuid: {" + entry.getUuid() + "}",
						Database.getThreadLocalGraph().v().has("uuid", entry.getUuid()).hasNext());
			}
			// 2. Check batch entries
			if (entry.getAction() != null) {
				if (!entry.getLanguages().isEmpty()) {
					// Check each language individually since the document id is constructed (uuid+lang)
					for (String language : entry.getLanguages()) {
						Optional<? extends SearchQueueEntry> batchEntry = batch.findEntryByUuid(entry.getUuid() + "-" + language);
						assertThat(batchEntry).as("Entry for {" + key + "}/{" + entry.getUuid() + "} - language {" + language + "}").isPresent();
						SearchQueueEntry batchEntryValue = batchEntry.get();
						assertEquals("The created batch entry for {" + key + "} language {" + language + "} did not use the expected action",
								entry.getAction(), batchEntryValue.getElementAction());
						nExpectedBatchEntries++;
					}
				} else {
					Optional<? extends SearchQueueEntry> batchEntry = batch.findEntryByUuid(entry.getUuid());
					assertThat(batchEntry).as("Entry for {" + key + "}/{" + entry.getUuid() + "}").isPresent();
					SearchQueueEntry batchEntryValue = batchEntry.get();
					assertEquals("The created batch entry for {" + key + "} did not use the expected action", entry.getAction(),
							batchEntryValue.getElementAction());
					nExpectedBatchEntries++;
				}
			}
		}
		if (batch.getEntries().size() != nExpectedBatchEntries) {
			for (SearchQueueEntry entry : batch.getEntries()) {
				System.out.println("Entry: " + entry.toString());
			}
			assertEquals("The search queue batch did not contain the amount of expected elements.", nExpectedBatchEntries, batch.getEntries().size());
		}
	}

}
