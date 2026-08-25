package com.gentics.mesh.plugin.forms;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import okhttp3.Request;

public class ResponsivenessTest extends AbstractFormsPluginIntegrationTest {

	static {
		server.getMeshOptions().getVertxOptions().setEventPoolSize(1);
	}

	@Test
	public void testBlockingGets() throws InterruptedException {
		int numThreads = 100;
		CountDownLatch latch = new CountDownLatch(numThreads);
		Request.Builder builder = new Request.Builder()
				.url(String.format("http://%s:%d/api/v1/auth/me", server.getHostname(), server.getPort()))
				.get();
		for (int i = 0; i < numThreads; i++) {
			new Thread(() -> {
				try {
					client().newCall(builder.build()).execute();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
				latch.countDown();
			}).start();
		}
		latch.await();
	}
}
