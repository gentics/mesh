package com.gentics.mesh.test.docker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.gentics.mesh.core.rest.MeshEvent;
import org.testcontainers.containers.output.OutputFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Latching consumer of log messages which will release the latch once the startup completed message has been seen in the log.
 */
public class StartupLatchingConsumer implements Consumer<OutputFrame> {

	private static Logger log = LoggerFactory.getLogger(StartupLatchingConsumer.class);

	private Runnable startupAction;

	public StartupLatchingConsumer() {
	}

	/**
	 * Create a new latching consumer which will invoke the given startup action once it has seen the startup log message.
	 * 
	 * @param startupAction
	 */
	public StartupLatchingConsumer(Runnable startupAction) {
		this.startupAction = startupAction;
	}

	private CountDownLatch latch = new CountDownLatch(1);

	@Override
	public void accept(OutputFrame frame) {
		if (frame != null) {
			String utf8String = frame.getUtf8String();
			if (utf8String.contains(MeshEvent.STARTUP.address)) {
				log.info("Startup message seen. Releasing lock.");
				if (startupAction != null) {
					startupAction.run();
				}
				latch.countDown();
			}
		}
	}

	/**
	 * Wait until the startup event has been received. The method will fail if the startup takes longer then expected.
	 * 
	 * @param timeoutValue
	 * @param unit
	 * @throws InterruptedException
	 */
	public void await(int timeoutValue, TimeUnit unit) throws InterruptedException {
		if (!latch.await(timeoutValue, unit)) {
			throw new UnresponsiveContainerError();
		} else {
			log.info("Started up.");
		}
	}

	/**
	 * The container did not respond in time, so this error is thrown.
	 * 
	 * @author plyhun
	 *
	 */
	public static class UnresponsiveContainerError extends RuntimeException {

		private static final long serialVersionUID = -2553839280282994621L;

		public UnresponsiveContainerError() {
			this("Container did not startup in time.");
		}
		public UnresponsiveContainerError(String message) {
			super(message);
		}
	}
}
