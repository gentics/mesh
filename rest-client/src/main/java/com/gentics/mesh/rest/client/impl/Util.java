package com.gentics.mesh.rest.client.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.Strings;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Collection of utility methods that are useful for {@link MeshRestClient} operation.
 */
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
		JsonNode jsonBody = JsonUtil.getMapper().valueToTree(body);
		return JsonUtil.getMapper().createObjectNode()
			.put("type", type.type)
			.put("address", address)
			.set("body", jsonBody)
			.toString();
	}

	/**
	 * A container for a value that is evaluated on demand. The supplier will be called at most once. After that, the received value is stored and returned on
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

	/**
	 * Return the array of event addresses from the given event enums.
	 * 
	 * @param events
	 * @return
	 */
	public static String[] toAddresses(MeshEvent... events) {
		return Stream.of(events)
			.map(MeshEvent::getAddress)
			.toArray(String[]::new);
	}

	/**
	 * Tests if the given value is positive. Throws an InvalidParameterException if not.
	 * 
	 * @param value
	 * @param name
	 */
	public static void requireNonNegative(long value, String name) {
		if (value < 0) {
			throw new InvalidParameterException(String.format("Parameter %s must not be negative. Given value: %d", name, value));
		}
	}

	/**
	 * Test whether the given value is a uuid. Throws an InvalidParameterException if not.
	 * @param value value to test
	 * @param name name of the parameter
	 */
	public static void requireUuid(String value, String name) {
		if (!UUIDUtil.isUUID(value)) {
			throw new InvalidParameterException("Parameter %s must be a uuid. Given value: %s".formatted(name, value));
		}
	}

	/**
	 * Safely append the given path to the prefix (which also is a path), so that the resulting path still is a subpath of the given prefix.
	 * If the composed path is no subpath of the given prefix, throws an InvalidParameterException.
	 * 
	 * @param prefix prefix
	 * @param path path to append
	 * @param name name of the parameter containing the path (for the description in the exception)
	 * @return composed path
	 */
	public static String safelyAppendPath(String prefix, String path, String name) {
		String composedPath = Strings.CI.removeEnd(prefix, "/") + Strings.CI.prependIfMissing(path, "/");

		try {
			String normalizedPath = normalizedPath(composedPath);

			if (!Strings.CI.startsWith(normalizedPath, prefix)) {
				throw new InvalidParameterException("Parameter %s is invalid. Given value: %s".formatted(name, path));
			}
		} catch (URISyntaxException e) {
			throw new InvalidParameterException("Parameter %s is invalid. Given value: %s".formatted(name, path));
		}

		return composedPath;
	}

	/**
	 * Normalize the path
	 * @param path path to normalize
	 * @return normalized path
	 * @throws URISyntaxException
	 */
	public static String normalizedPath(String path) throws URISyntaxException {
		String normalizedURI = new URI("http://host%s".formatted(Strings.CI.prependIfMissing(path, "/"))).normalize().toString();

		return Strings.CI.removeStart(normalizedURI, "http://host");
	}

	interface WrappedSupplier<T> {
		T get() throws Exception;
	}
}
