package com.gentics.cailun.cli;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.gentics.cailun.etc.config.CaiLunConfiguration;
import com.gentics.cailun.test.AbstractIntegrationTest;
import com.gentics.cailun.test.TestUtil;

public class CaiLunIntegerationTest extends AbstractIntegrationTest {

	@Test
	public void testStartup() throws Exception {
		CaiLunConfiguration config = new CaiLunConfiguration();
		config.setHttpPort(TestUtil.getRandomPort());
		config.getNeo4jConfiguration().setMode(Neo4jGraphVerticle.DEFAULT_MODE);

		final CaiLun cailun = CaiLun.getInstance();
		final AtomicBoolean customLoaderInvoked = new AtomicBoolean(false);
		final AtomicBoolean caiLunStarted = new AtomicBoolean(false);
		cailun.setCustomLoader((vertx) -> {
			// deployAndWait(vertx, CustomerVerticle.class);
			customLoaderInvoked.set(true);
		});
		final CountDownLatch latch = new CountDownLatch(1);

		new Thread(() -> {
			try {
				cailun.run(config, () -> {
					assertTrue("The custom loader was not invoked during the startup process", customLoaderInvoked.get());
					caiLunStarted.set(true);
					latch.countDown();
				});
			} catch (Exception e) {
				fail("Error while starting instance: " + e.getMessage());
				e.printStackTrace();
			}
		}).start();
		if (latch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
			assertTrue(caiLunStarted.get());
		} else {
			fail("Cailun did not startup on time. Timeout {" + DEFAULT_TIMEOUT_SECONDS + "} seconds reached.");
		}
	}
}
