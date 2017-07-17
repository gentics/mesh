package com.gentics.mesh.distributed;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.testcontainers.containers.output.OutputFrame;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class StartupLatchingConsumer implements Consumer<OutputFrame> {

	private static Logger log = LoggerFactory.getLogger(StartupLatchingConsumer.class);
	private int timeoutInSeconds;

	public StartupLatchingConsumer(int timeoutInSeconds) {
		this.timeoutInSeconds = timeoutInSeconds;
	}

	private CountDownLatch latch = new CountDownLatch(1);

	@Override
	public void accept(OutputFrame frame) {
		if (frame != null) {
			String utf8String = frame.getUtf8String();
			if (utf8String.contains("mesh-startup-complete")) {
				log.info("Startup message seen. Releasing lock");
				latch.countDown();
			}
		}
	}

	/**
	 * Wait until the startup event has been received. The method will fail if the startup takes longer then expected.
	 * 
	 * @throws InterruptedException
	 */
	public void await() throws InterruptedException {
		if (!latch.await(timeoutInSeconds, TimeUnit.SECONDS)) {
			throw new RuntimeException("Container did not startup in time.");
		}
	}

}
