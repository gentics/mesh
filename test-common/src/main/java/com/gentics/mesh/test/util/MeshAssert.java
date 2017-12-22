package com.gentics.mesh.test.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.rest.client.MeshResponse;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class MeshAssert {

	private static final Logger log = LoggerFactory.getLogger(MeshAssert.class);

	private static final Integer CI_TIMEOUT_SECONDS = 100;

	private static final Integer DEV_TIMEOUT_SECONDS = 10000;

	public static void assertSuccess(MeshResponse<?> future) {
		if (future.cause() != null) {
			future.cause().printStackTrace();
		}
		assertTrue("The future failed with error {" + (future.cause() == null ? "Unknown error" : future.cause().getMessage()) + "}",
				future.succeeded());
	}

	public static void assertElement(RootVertex<?> root, String uuid, boolean exists) throws Exception {
		Object element = root.findByUuid(uuid);
		if (exists) {
			assertNotNull("The element should exist.", element);
		} else {
			assertNull("The element should not exist.", element);
		}

	}

	public static int getTimeout() throws UnknownHostException {
		int timeout = CI_TIMEOUT_SECONDS;
		if (TestUtils.isHost("plexus") || TestUtils.isHost("corvus.lan.apa.at")) {
			timeout = DEV_TIMEOUT_SECONDS;
		}
		if (log.isDebugEnabled()) {
			log.debug("Using test timeout of {" + timeout + "} seconds for host {" + TestUtils.getHostname() + "}");
		}
		return timeout;
	}

	private static void printAllStackTraces() {
		Map<Thread, StackTraceElement[]> liveThreads = Thread.getAllStackTraces();
		for (Iterator<Thread> i = liveThreads.keySet().iterator(); i.hasNext();) {
			Thread key = i.next();
			System.err.println("Thread " + key.getName());
			StackTraceElement[] trace = liveThreads.get(key);
			for (int j = 0; j < trace.length; j++) {
				System.err.println("\tat " + trace[j]);
			}
		}
	}

	public static void latchFor(MeshResponse<?> future) {
		CountDownLatch latch = new CountDownLatch(1);
		future.setHandler(rh -> {
			latch.countDown();
		});
		try {
			if (!latch.await(getTimeout(), TimeUnit.SECONDS)) {
				printAllStackTraces();
				fail("The timeout of the latch was reached.");
			}

		} catch (UnknownHostException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void failingLatch(CountDownLatch latch, int timeoutInSeconds) throws InterruptedException {
		if (!latch.await(timeoutInSeconds, TimeUnit.SECONDS)) {
			printAllStackTraces();
			fail("Latch timeout reached");
		}
	}

	public static void failingLatch(CountDownLatch latch) throws Exception {
		if (!latch.await(getTimeout(), TimeUnit.SECONDS)) {
			printAllStackTraces();
			fail("Latch timeout reached {" + getTimeout() + "} seconds.");
		}
	}

}
