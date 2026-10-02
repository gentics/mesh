package com.gentics.mesh.test.context.helper;

import static org.junit.Assert.assertSame;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.event.EventAsserter;
import com.gentics.mesh.test.helper.ExpectedEvent;

import io.vertx.core.Vertx;

/**
 * Tests for {@link EventHelper}.
 * The code passed to the helpers typically does blocking calls (e.g. REST calls), so it must be executed on the calling thread and never on an event
 * loop thread.
 */
public class EventHelperTest implements EventHelper {

	private static final String ADDRESS = "mesh.test.eventhelper";

	private Vertx vertx;

	@Before
	public void setup() {
		vertx = Vertx.vertx();
	}

	@After
	public void tearDown() {
		vertx.close().await();
	}

	@Override
	public MeshTestContext getTestContext() {
		throw new UnsupportedOperationException("Not needed for this test");
	}

	@Override
	public EventAsserter eventAsserter() {
		throw new UnsupportedOperationException("Not needed for this test");
	}

	@Override
	public Vertx vertx() {
		return vertx;
	}

	@Test
	public void testWaitForEventRunsCodeOnCallingThread() {
		Thread caller = Thread.currentThread();
		AtomicReference<Thread> executor = new AtomicReference<>();
		waitForEvent(ADDRESS, () -> {
			executor.set(Thread.currentThread());
			vertx.eventBus().publish(ADDRESS, null);
		}, 1_000);
		assertSame("The code must be executed on the calling thread", caller, executor.get());
	}

	@Test
	public void testExpectEventRunsCodeOnCallingThread() throws Exception {
		Thread caller = Thread.currentThread();
		AtomicReference<Thread> executor = new AtomicReference<>();
		try (ExpectedEvent ee = expectEvent(ADDRESS, () -> {
			executor.set(Thread.currentThread());
			vertx.eventBus().publish(ADDRESS, null);
		}, 1_000)) {
		}
		assertSame("The code must be executed on the calling thread", caller, executor.get());
	}
}
