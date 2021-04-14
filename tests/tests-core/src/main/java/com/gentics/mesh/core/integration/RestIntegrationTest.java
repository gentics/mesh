package com.gentics.mesh.core.integration;

import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.test.AbstractIntegrationTest;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Ignore
public class RestIntegrationTest extends AbstractIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(RestIntegrationTest.class);

	@Test
	public void testIntegration() throws Exception {
		long timeout = DEFAULT_TIMEOUT_SECONDS * 2;
		final CountDownLatch latch = new CountDownLatch(1);

		final Mesh mesh = Mesh.create();
		mesh.setCustomLoader((vertx) -> {
			vertx.eventBus().consumer("mesh-startup-complete", mh -> {
				log.info("Received startup event..");
				latch.countDown();
			});
		});

		new Thread(() -> {
			try {
				mesh.run();
			} catch (Exception e) {
				fail("Error while starting instance: " + e.getMessage());
				e.printStackTrace();
			}
		}).start();
		if (!latch.await(timeout, TimeUnit.SECONDS)) {
			fail("Mesh did not startup on time. Timeout {" + timeout + "} seconds reached.");
		}
	}
}
