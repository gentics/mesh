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

import com.gentics.mesh.search.verticle.eventhandler.Util;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EventAsserter {

	private static final Logger log = LoggerFactory.getLogger(EventAsserter.class);

	private Map<CompletableFuture<Void>, MeshEvent> futures = new HashMap<>();

	private Map<MeshEvent, List<JsonObject>> events = new ConcurrentHashMap<>();

	private Subject<Object> eventSubject = PublishSubject.create();

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
		log.info("Waiting for events...");
		// Wait for all events with a timeout between events of 500 ms.
		eventSubject
			.timeout(500, TimeUnit.MILLISECONDS)
			.onErrorResumeNext((Throwable err) -> {
				if (err instanceof TimeoutException) {
					return Observable.empty();
				} else {
					return Observable.error(err);
				}
			})
			.ignoreElements().blockingAwait();

		log.info("Done waiting for events");

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
				JsonObject body = mh.body();
				list.add(body);
				eventSubject.onNext(Util.dummyObject);
				fut.complete(null);
			});
			futures.put(fut, event);
		}

	}

	public void addExpectation(EventExpectation expectation) {
		expectations.add(expectation);
	}

}
