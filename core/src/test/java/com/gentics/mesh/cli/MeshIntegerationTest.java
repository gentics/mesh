package com.gentics.mesh.cli;

import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.test.AbstractIntegrationTest;
import com.gentics.mesh.test.SpringTestConfiguration;

public class MeshIntegerationTest extends AbstractIntegrationTest {

	@Test
	public void testStartup() throws Exception {

		SpringTestConfiguration.ignored = true;
		long timeout = DEFAULT_TIMEOUT_SECONDS * 6;

		final CountDownLatch latch = new CountDownLatch(2);
		final Mesh mesh = Mesh.mesh();
		mesh.getVertx().eventBus().consumer(Mesh.STARTUP_EVENT_ADDRESS, mh -> {
			latch.countDown();
		});
		mesh.setCustomLoader((vertx) -> {
			latch.countDown();
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
