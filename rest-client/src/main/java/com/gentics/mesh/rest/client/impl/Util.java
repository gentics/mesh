package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.MeshEvent;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Util {
	private Util() {
	}

	/**
	 * Creates a Vert.x event bus message to be send over a websocket.
	 *
	 * @see com.gentics.mesh.rest.client.MeshWebsocket
	 *
	 * @param type
	 * @return
	 */
	public static String eventbusMessage(EventbusMessageType type) {
		try {
			return new JSONObject()
				.put("type", type.type)
				.toString();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a Vert.x event bus message to be send over a websocket.
	 *
	 * @see com.gentics.mesh.rest.client.MeshWebsocket
	 *
	 * @param type
	 * @param address
	 * @return
	 */
	public static String eventbusMessage(EventbusMessageType type, String address) {
		try {
			return new JSONObject()
				.put("type", type.type)
				.put("address", address)
				.toString();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a Vert.x event bus message to be send over a websocket.
	 *
	 * @see com.gentics.mesh.rest.client.MeshWebsocket
	 *
	 * @param type
	 * @param address
	 * @param body
	 * @return
	 */
	public static String eventbusMessage(EventbusMessageType type, String address, Object body) {
		try {
			return new JSONObject()
				.put("type", type.type)
				.put("address", address)
				.put("body", body)
				.toString();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A container for a value that is evaluated on demand.
	 * The supplier will be called at most once. After that, the received value is stored and returned on
	 * subsequent calls of {@link Supplier#get()}
	 *
	 * @param supplier
	 * @param <T>
	 * @return
	 */
	public static <T> Supplier<T> lazily(WrappedSupplier<T> supplier) {
		return new Supplier<T>() {
			T value;
			boolean supplied = false;

			@Override
			public synchronized T get() {
				if (!supplied) {
					try {
						value = supplier.get();
						supplied = true;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				return value;
			}
		};
	}

	public static String[] toAddresses(MeshEvent... events) {
		return Stream.of(events)
			.map(MeshEvent::getAddress)
			.toArray(String[]::new);
	}

	interface WrappedSupplier<T> {
		T get() throws Exception;
	}
}
