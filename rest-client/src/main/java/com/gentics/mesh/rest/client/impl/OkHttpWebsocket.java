package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.rest.client.EventbusEvent;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.MeshWebsocket;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.gentics.mesh.rest.client.impl.Util.eventbusMessage;

public class OkHttpWebsocket implements MeshWebsocket {
	private static final Logger log = LoggerFactory.getLogger(OkHttpWebsocket.class);

	private final OkHttpClient client;
	private final MeshRestClientConfig config;

	private static final Object connectionDummy = new Object();

	private final Subject<EventbusEvent> events = PublishSubject.create();
	private final Subject<Object> connections = PublishSubject.create();
	private final Subject<Throwable> errors = PublishSubject.create();
	private final Set<String> registeredEventAddresses = Collections.synchronizedSet(new HashSet<>());

	private WebSocket currentConnection;
	private AtomicBoolean connected = new AtomicBoolean(false);
	private Disposable pingInterval;

	public OkHttpWebsocket(OkHttpClient client, MeshRestClientConfig config) {
		this.client = client;
		this.config = config;

		connect();
		startPings();
		errors.subscribe(err -> log.error("Error in Websocket", err));
	}

	private void connect() {
		Request request = new Request.Builder()
			.url(config.getBaseUrl() + "/eventbus/websocket")
			.build();

		connected.set(false);

		currentConnection = client.newWebSocket(request, new WebSocketListener() {
			@Override
			public void onOpen(WebSocket webSocket, Response response) {
				connected.set(true);
				sendRegisterEvents();
				connections.onNext(connectionDummy);
			}

			@Override
			public void onMessage(WebSocket webSocket, String text) {
				try {
					events.onNext(new EventbusEvent(text));
				} catch (IOException e) {
					errors.onNext(new Exception("Could not parse message from mesh", e));
				}
			}

			@Override
			public void onMessage(WebSocket webSocket, ByteString bytes) {
				onMessage(webSocket, bytes.utf8());
			}

			@Override
			public void onClosing(WebSocket webSocket, int code, String reason) {
				// Mesh does not close the socket
				errors.onNext(new Exception(String.format("Unexpected closing of socket by peer. Code: %d, reason: %s", code, reason)));
				reconnect();
			}

			@Override
			public void onClosed(WebSocket webSocket, int code, String reason) {
				// Mesh does not close the socket
				errors.onNext(new Exception(String.format("Unexpected closed socket. Code: %d, reason: %s", code, reason)));
				reconnect();
			}

			@Override
			public void onFailure(WebSocket webSocket, Throwable t, Response response) {
				errors.onNext(new Exception("Failure in websocket to mesh", t));
				reconnect();
			}
		});
	}

	private void startPings() {
		pingInterval = Observable.interval(config.getWebsocketReconnectInterval().toMillis(), TimeUnit.MILLISECONDS)
			.subscribe(ignore -> send(eventbusMessage(EventbusMessageType.PING)));
	}

	private void reconnect() {
		if (connected.compareAndSet(true, false)) {
			Completable.complete()
				.delay(config.getWebsocketReconnectInterval().toMillis(), TimeUnit.MILLISECONDS)
				.subscribe(this::connect);
		}
	}

	@Override
	public void close() {
		pingInterval.dispose();
		events.onComplete();
		connections.onComplete();
		errors.onComplete();
	}

	@Override
	public void publishEvent(String eventName, Object body) {
		send(eventbusMessage(EventbusMessageType.PUBLISH, eventName, body));
	}

	@Override
	public void registerEvents(String... eventNames) {
		registeredEventAddresses.addAll(Arrays.asList(eventNames));
		sendRegisterEvents();
	}

	@Override
	public void unregisterEvents(String... eventNames) {
		Stream.of(eventNames).forEach(registeredEventAddresses::remove);
	}

	private void sendRegisterEvents() {
		registeredEventAddresses.forEach(address -> send(eventbusMessage(EventbusMessageType.REGISTER, address)));
	}

	private void send(String text) {
		if (connected.get()) {
			currentConnection.send(text);
		}
	}

	@Override
	public Observable<EventbusEvent> events() {
		return events;
	}

	@Override
	public Observable<Object> connections() {
		return connections
			// If already connected, emit the connectionDummy once. Similar to BehaviourSubject, but this does not
			// emit an event if the socket is disconnected.
			.startWith(Maybe.create(sub -> {
				if (connected.get()) {
					sub.onSuccess(connectionDummy);
				} else {
					sub.onComplete();
				}
			}).toObservable());
	}

	@Override
	public Observable<Throwable> errors() {
		return errors;
	}
}
