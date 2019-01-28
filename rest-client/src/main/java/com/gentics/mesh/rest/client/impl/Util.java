package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.MeshEvent;
import com.gentics.mesh.json.JsonUtil;

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
		return JsonUtil.getMapper().createObjectNode()
			.put("type", type.type)
			.toString();
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
		return JsonUtil.getMapper().createObjectNode()
			.put("type", type.type)
			.put("address", address)
			.toString();
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
		return JsonUtil.getMapper().createObjectNode()
			.put("type", type.type)
			.put("address", address)
			.put("body", JsonUtil.toJson(body))
			.toString();
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
