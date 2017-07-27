package com.gentics.mesh.rest.client.handler;

import com.gentics.mesh.rest.client.MeshResponse;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

/**
 * @param <T>
 *            Success response type
 */
public interface ResponseHandler<T> extends Handler<HttpClientResponse> {

	/**
	 * Return the mesh response which may contain the response object.
	 * 
	 * @return
	 */
	MeshResponse<T> getFuture();

	/**
	 * Return the origin request URI.
	 * 
	 * @return
	 */
	String getUri();

	/**
	 * Return the origin request method.
	 * 
	 * @return
	 */
	HttpMethod getMethod();

	@Override
	default void handle(HttpClientResponse response) {
		getFuture().setRawResponse(response);
		int code = response.statusCode();
		if (code >= 200 && code < 300) {
			handleSuccess(response);
		} else if (code == 304) {
			handleNotModified(response);
		} else {
			handleError(response);
		}
	}

	/**
	 * Error method which will be invoked for responses which are in between 200 and 300.
	 * 
	 * @param response
	 */
	void handleError(HttpClientResponse response);

	/**
	 * Success method which will be invoked for responses with codes &gt;=200 &amp;&amp; &gt; 300.
	 * 
	 * @param response
	 */
	void handleSuccess(HttpClientResponse response);

	/**
	 * Handles 304 responses.
	 * 
	 * @param response
	 */
	default void handleNotModified(HttpClientResponse response) {
		getFuture().complete(null);
	}

}
