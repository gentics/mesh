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

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class EventAsserter {

	private Map<CompletableFuture<Void>, MeshEvent> futures = new HashMap<>();

	private Map<MeshEvent, List<JsonObject>> events = new ConcurrentHashMap<>();

	private List<EventExpectation> expectations = new ArrayList<>();

	/**
	 * Start an expectation chain.
	 * 
	 * @param event
	 * @return
	 */
	public EventAsserterChain expect(MeshEvent event) {
		registerForEvent(event);
		return new EventAsserterChain(this, event);
	}

	/**
	 * Wait for events and assert the expectations.
	 */
	public void await() {
		for (Entry<CompletableFuture<Void>, MeshEvent> entry : futures.entrySet()) {
			try {
				entry.getKey().get(500, TimeUnit.MILLISECONDS);
			} catch (ExecutionException | TimeoutException | InterruptedException e) {
				// Ignored
			}
		}
		for (EventExpectation expectation : expectations) {
			expectation.verify(events);
		}

		clear();
	}

	/**
	 * Clear all expectations and registered events.
	 */
	public void clear() {
		futures.clear();
		events.clear();
		expectations.clear();
	}

	public void registerForEvent(MeshEvent event) {
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

	public void addExpectation(EventExpectation expectation) {
		expectations.add(expectation);
	}

}
