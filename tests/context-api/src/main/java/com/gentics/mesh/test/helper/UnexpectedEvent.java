package com.gentics.mesh.test.helper;

import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;

/**
 * AutoClosable implementation, which will register an event handler upon
 * creation and will wait (up to the given timeout) in
 * {@link UnexpectedEvent#close()} for the event to be fired. If the event was fired, an exception is thrown.
 * See {@link EventHelper#notExpectEvent(com.gentics.mesh.core.rest.MeshEvent, int)} for usage.
 */

public class UnexpectedEvent implements AutoCloseable {
	protected CountDownLatch latch = new CountDownLatch(1);

	protected String address;

	protected int timeoutMs;

	protected MessageConsumer<Object> consumer;

	/**
	 * Create an instance and register the event handler
	 * @param vertx vertx instance
	 * @param address event address
	 * @param timeoutMs timeout in milliseconds
	 */
	public UnexpectedEvent(Vertx vertx, String address, int timeoutMs) {
		this.address = address;
		this.timeoutMs = timeoutMs;
		consumer = vertx.eventBus().consumer(address);
		consumer.handler(msg -> latch.countDown());
	}

	@Override
	public void close() {
		try {
			if (latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
				fail("Event " + address + " was handled at least once within timeout");
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			consumer.unregister();
		}
	}
}
