package com.gentics.mesh.rest.client;

import java.util.List;
import java.util.Map;

public interface MeshResponse2<T> {
	/**
	 * Retrieve the response headers
	 * @return A map of all response headers
	 */
	Map<String, List<String>> getHeaders();

	/**
	 * Retrieve a header from the response.
	 * @param name The name of the header
	 * @return The value of the header
	 */
	default List<String> getHeaders(String name) {
		return getHeaders().get(name);
	}

	/**
	 * Retrieve a header from the response.
	 * @param name The name of the header
	 * @return The value of the first header with the given name
	 */
	default String getHeader(String name) {
		return getHeaders(name).get(0);
	}

	/**
	 * Retrieves the status code of the response
	 * @return The status code
	 */
	int getStatusCode();

	/**
	 * Retrieves the entire response body. Blocks until the entire body is received.
	 * Use with caution when receiving large responses, because the entire body is stored in memory.
	 * @return The entire response body
	 */
	String getBodyAsString();

	/**
	 * Retrieves the entire response body.
	 * @return The body as the specified type
	 */
	T getBody();
}
