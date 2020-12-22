package com.gentics.mesh.rest.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Common interface for Gentics Mesh REST Client responses.
 * 
 * @param <T>
 *            Response type
 */
public interface MeshResponse<T> {
	/**
	 * Retrieve the response headers
	 * 
	 * @return A map of all response headers
	 */
	Map<String, List<String>> getHeaders();

	/**
	 * Retrieve a header from the response.
	 * 
	 * @param name
	 *            The name of the header
	 * @return The value of the header
	 */
	default List<String> getHeaders(String name) {
		List<String> headers = getHeaders().get(name);
		return headers == null
			? Collections.emptyList()
			: headers;
	}

	/**
	 * Retrieve a header from the response.
	 * 
	 * @param name
	 *            The name of the header
	 * @return The value of the first header with the given name
	 */
	default Optional<String> getHeader(String name) {
		List<String> headers = getHeaders(name);
		return headers.size() == 0
			? Optional.empty()
			: Optional.of(headers.get(0));
	}

	/**
	 * Retrieve the Set-Cookie headers.
	 * 
	 * @return A list of Set-Cookie directives
	 */
	default List<String> getCookies() {
		return getHeaders("Set-Cookie");
	}

	/**
	 * Retrieves the status code of the response
	 * 
	 * @return The status code
	 */
	int getStatusCode();

	/**
	 * Retrieves the entire response body. Blocks until the entire body is received. Use with caution when receiving large responses, because the entire body is
	 * stored in memory.
	 * 
	 * @return The entire response body
	 */
	String getBodyAsString();

	/**
	 * Retrieves the entire response body.
	 * 
	 * @return The body as the specified type
	 */
	T getBody();
}
