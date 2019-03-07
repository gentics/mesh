package com.gentics.mesh.test.context.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshEventModel;

import io.reactivex.functions.Predicate;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class EventAsserter {

	private Map<CompletableFuture<Void>, MeshEvent> futures = new HashMap<>();

	private Map<MeshEvent, List<JsonObject>> events = new ConcurrentHashMap<>();

	private List<EventExpectation> expectations = new ArrayList<>();

	/**
	 * Add an expectation for an event body.
	 * 
	 * @param event
	 * @param expectedCount
	 *            How many events should be passed by the asserter
	 * @param clazzOfEM
	 * @param asserter
	 * @return
	 */
	public <EM extends MeshEventModel> EventAsserter expect(MeshEvent event, int expectedCount, Class<EM> clazzOfEM,
		Predicate<EM> asserter) {
		expectations.add(new EventBodyExpectation(event, expectedCount, clazzOfEM, asserter));
		registerForEvent(event);
		return this;
	}

	/**
	 * Add an expectation for event total count.
	 * 
	 * @param event
	 * @param expectedCount
	 * @return
	 */
	public EventAsserter expect(MeshEvent event, int expectedCount) {
		expectations.add(new EventCountExpectation(event, expectedCount));
		registerForEvent(event);
		return this;
	}

	/**
	 * Wait for events and assert the expectations.
	 */
	public void await() {
		for (Entry<CompletableFuture<Void>, MeshEvent> entry : futures.entrySet()) {
			MeshEvent event = entry.getValue();
			try {
				entry.getKey().get(4, TimeUnit.SECONDS);
			} catch (ExecutionException | TimeoutException | InterruptedException e) {
				if (e instanceof ExecutionException) {
					Throwable cause = e.getCause();
					if (cause instanceof AssertionError) {
						throw (AssertionError) cause;
					}
				}
				throw new RuntimeException("Did not receive event for {" + event.getAddress() + "}", e);
			}
		}
		for (EventExpectation expectation : expectations) {
			expectation.verify(events);
		}

		clear();
	}

	/**
	 * Clear all expectations and registerd events.
	 */
	public void clear() {
		futures.clear();
		events.clear();
		expectations.clear();
	}

	private void registerForEvent(MeshEvent event) {
		// We don't need to listen to events multiple times.
		boolean isRegistered = events.containsKey(event);
		if (!isRegistered) {
			CompletableFuture<Void> fut = new CompletableFuture<>();
			List<JsonObject> list = events.computeIfAbsent(event, e -> new ArrayList<>());
			Mesh.vertx().eventBus().consumer(event.getAddress(), (Message<JsonObject> mh) -> {
				// Add the event to the list of events
				list.add(mh.body());
				fut.complete(null);
			});
			futures.put(fut, event);
		}

	}

}
