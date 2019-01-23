package com.gentics.mesh.rest.client;

import com.gentics.mesh.MeshEvent;
import io.reactivex.Observable;

import static com.gentics.mesh.rest.client.impl.Util.toAddresses;

/**
 * An open websocket to mesh.
 */
public interface MeshWebsocket {

	/**
	 * Closes the connection to mesh and emits a complete event on all available observables.
	 */
	void close();

	/**
	 * Publishes an event.
	 *
	 * @param eventName The name of the event
	 * @param body The message of the event. Must be a Json serializable object.
	 */
	void publishEvent(String eventName, Object body);

	/**
	 * Registers on an event so that this websocket will receive messages for the given addresses.
	 * Subscribe {@link #events()} to react to incoming events.
	 *
	 * @param eventNames The names of the events to register to
	 */
	void registerEvents(String... eventNames);

	/**
	 * Registers on an event so that this websocket will receive messages for the given addresses.
	 * Subscribe {@link #events()} to react to incoming events.
	 *
	 * @param events The events to register to
	 */
	default void registerEvents(MeshEvent... events) {
		registerEvents(toAddresses(events));
	}

	/**
	 * Unregisters on events.
	 * The {@link #events()} observable will stop receiving events of the given addresses.
	 *
	 * @param eventNames The names of the events to register to
	 */
	void unregisterEvents(String... eventNames);

	/**
	 * Unregisters on events.
	 * The {@link #events()} observable will stop receiving events of the given addresses.
	 *
	 * @param events The events to register to
	 */
	default void unregisterEvents(MeshEvent... events) {
		unregisterEvents(toAddresses(events));
	}

	/**
	 * A
	 * <a href="https://github.com/Froussios/Intro-To-RxJava/blob/master/Part%203%20-%20Taming%20the%20sequence/6.%20Hot%20and%20Cold%20observables.md#hot-observables">hot observable</a>
	 * that emits incoming events from mesh. Multiple subscriptions will not affect the websocket in any way.
	 */
	Observable<EventbusEvent> events();

	/**
	 * A
	 * <a href="https://github.com/Froussios/Intro-To-RxJava/blob/master/Part%203%20-%20Taming%20the%20sequence/6.%20Hot%20and%20Cold%20observables.md#hot-observables">hot observable</a>
	 * that emits a dummy Object whenever the client connects or reconnects to mesh. Multiple subscriptions will not affect the websocket in any way.
	 *
	 * <p>This is useful when the websocket connection is unstable and and reconnects are expected.</p>
	 */
	Observable<Object> connections();

	/**
	 * A
	 * <a href="https://github.com/Froussios/Intro-To-RxJava/blob/master/Part%203%20-%20Taming%20the%20sequence/6.%20Hot%20and%20Cold%20observables.md#hot-observables">hot observable</a>
	 * that emits any errors encountered when trying to connect to Mesh. Multiple subscriptions will not affect the websocket in any way.
	 *
	 * <p>This is useful when the websocket connection is unstable and and reconnects are expected.</p>
	 */
	Observable<Throwable> errors();
}
