package com.gentics.mesh.cli;

import static com.gentics.mesh.MeshEvent.STARTUP;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.test.AbstractIntegrationTest;

@Ignore
public class MeshIntegerationTest extends AbstractIntegrationTest {

	@Before
	public void cleanup() throws IOException {
		new File("mesh.json").delete();
		FileUtils.deleteDirectory(new File("data"));
	}

	@Test
	public void testStartup() throws Exception {

		long timeout = DEFAULT_TIMEOUT_SECONDS * 20;
		final CountDownLatch latch = new CountDownLatch(2);
		final Mesh mesh = Mesh.mesh();
		mesh.getVertx().eventBus().consumer(STARTUP.address, mh -> {
			latch.countDown();
		});
		mesh.setCustomLoader((vertx) -> {
			latch.countDown();
		});

		new Thread(() -> {
			try {
				mesh.run();
			} catch (Exception e) {
				e.printStackTrace();
				fail("Error while starting instance: " + e.getMessage());
			}
		}).start();
		if (!latch.await(timeout, TimeUnit.SECONDS)) {
			fail("Mesh did not startup on time. Timeout {" + timeout + "} seconds reached.");
		}
	}
}
