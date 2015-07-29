package com.gentics.mesh.core.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.test.AbstractIntegrationTest;

public class RestIntegrationTest extends AbstractIntegrationTest {

	@Test
	public void testIntegration() throws Exception {
		long timeout = DEFAULT_TIMEOUT_SECONDS * 2;

		final Mesh mesh = Mesh.mesh();
		final AtomicBoolean customLoaderInvoked = new AtomicBoolean(false);
		final AtomicBoolean meshStarted = new AtomicBoolean(false);
		mesh.setCustomLoader((vertx) -> {
			
			vertx.eventBus().consumer("mesh-startup-complete", mh -> {
				System.out.println("Jow");
			});

			customLoaderInvoked.set(true);
		});
		final CountDownLatch latch = new CountDownLatch(1);

		new Thread(() -> {
			try {
				mesh.run(() -> {
					assertTrue("The custom loader was not invoked during the startup process", customLoaderInvoked.get());
					meshStarted.set(true);
					latch.countDown();
				});
			} catch (Exception e) {
				fail("Error while starting instance: " + e.getMessage());
				e.printStackTrace();
			}
		}).start();
		if (latch.await(timeout, TimeUnit.SECONDS)) {
			assertTrue(meshStarted.get());
		} else {
			fail("Mesh did not startup on time. Timeout {" + timeout + "} seconds reached.");
		}
	}
}
