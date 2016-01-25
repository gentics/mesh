package com.gentics.mesh.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.TestUtil;

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
		if (TestUtil.isHost("plexus") || TestUtil.isHost("satan3.office")) {
			timeout = DEV_TIMEOUT_SECONDS;
		}
		if (log.isDebugEnabled()) {
			log.debug("Using test timeout of {" + timeout + "} seconds for host {" + TestUtil.getHostname() + "}");
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
		}
		catch (UnknownHostException | InterruptedException e) {
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

	public static void assertDeleted(Map<String, String> uuidToBeDeleted) {
		for (Map.Entry<String, String> entry : uuidToBeDeleted.entrySet()) {
			assertFalse("One vertex was not deleted. Uuid: {" + entry.getValue() + "} - Type: {" + entry.getKey() + "}",
					Database.getThreadLocalGraph().v().has("uuid", entry.getValue()).hasNext());
		}
	}

}
