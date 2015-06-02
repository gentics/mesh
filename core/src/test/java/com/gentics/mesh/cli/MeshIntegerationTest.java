package com.gentics.mesh.cli;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.gentics.mesh.etc.config.MeshConfiguration;
import com.gentics.mesh.test.AbstractIntegrationTest;
import com.gentics.mesh.test.TestUtil;

public class MeshIntegerationTest extends AbstractIntegrationTest {

	@Test
	public void testStartup() throws Exception {
		long timeout = DEFAULT_TIMEOUT_SECONDS * 2;
		MeshConfiguration config = new MeshConfiguration();
		config.setHttpPort(TestUtil.getRandomPort());
		config.getNeo4jConfiguration().setMode(Neo4jGraphVerticle.DEFAULT_MODE);

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
		if (latch.await(timeout, TimeUnit.SECONDS)) {
			assertTrue(meshStarted.get());
		} else {
			fail("Mesh did not startup on time. Timeout {" + timeout + "} seconds reached.");
		}
	}
}
