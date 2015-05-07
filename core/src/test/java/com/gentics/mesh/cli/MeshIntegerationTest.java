package com.gentics.mesh.cli;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.gentics.mesh.etc.config.MeshConfiguration;
import com.gentics.mesh.etc.config.MeshNeo4jConfiguration;
import com.gentics.mesh.test.AbstractIntegrationTest;
import com.gentics.mesh.test.TestUtil;

public class MeshIntegerationTest extends AbstractIntegrationTest {

	@Test
	public void testStartup() throws Exception {
		MeshConfiguration config = new MeshConfiguration();
		config.setHttpPort(TestUtil.getRandomPort());
		config.getNeo4jConfiguration().setMode(MeshNeo4jConfiguration.DEFAULT_MODE);

		final Mesh mesh = Mesh.mesh();
		final AtomicBoolean customLoaderInvoked = new AtomicBoolean(false);
		final AtomicBoolean meshStarted = new AtomicBoolean(false);
		mesh.setCustomLoader((vertx) -> {
			// deployAndWait(vertx, CustomerVerticle.class);
			customLoaderInvoked.set(true);
		});
		final CountDownLatch latch = new CountDownLatch(1);

		new Thread(() -> {
			try {
				mesh.run(config, () -> {
					assertTrue("The custom loader was not invoked during the startup process", customLoaderInvoked.get());
					meshStarted.set(true);
					latch.countDown();
				});
			} catch (Exception e) {
				fail("Error while starting instance: " + e.getMessage());
				e.printStackTrace();
			}
		}).start();
		if (latch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
			assertTrue(meshStarted.get());
		} else {
			fail("Mesh did not startup on time. Timeout {" + DEFAULT_TIMEOUT_SECONDS + "} seconds reached.");
		}
	}
}
