package com.gentics.mesh.test.helper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.functions.Action;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;

/**
 * AutoClosable implementation, which will register an event handler upon
 * creation and will wait (up to the given timeout) in
 * {@link ExpectedEvent#close()} for the event to be fired at least once.
 * See {@link EventHelper#expectEvent(com.gentics.mesh.core.rest.MeshEvent, int)} for usage.
 */
public class ExpectedEvent implements AutoCloseable {
	protected CountDownLatch latch = new CountDownLatch(1);

	protected int timeoutMs;

	protected MessageConsumer<Object> consumer;

	/**
	 * Create an instance and register event handler
	 * @param vertx vertx instance
	 * @param address event address
	 * @param code code to be executed when the event was fired
	 * @param timeoutMs timeout in milliseconds
	 */
	public ExpectedEvent(Vertx vertx, String address, Action code, int timeoutMs) {
		this.timeoutMs = timeoutMs;
		consumer = vertx.eventBus().consumer(address);
		consumer.handler(msg -> latch.countDown());
		try {
			// Wait until the consumer has been registered and execute the code on the calling thread.
			// The code must not be executed on an event loop thread, because it typically does blocking calls.
			consumer.completion().toCompletionStage().toCompletableFuture().get(timeoutMs, TimeUnit.MILLISECONDS);
			code.run();
		} catch (Exception e) {
			consumer.unregister();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws TimeoutException {
		try {
			if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
				throw new TimeoutException("Timeout while waiting for event");
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			consumer.unregister();
		}
	}
}
