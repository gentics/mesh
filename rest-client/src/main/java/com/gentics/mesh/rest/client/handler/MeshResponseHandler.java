package com.gentics.mesh.rest.client.handler;

import com.gentics.mesh.rest.client.MeshResponse;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

public interface MeshResponseHandler<T> extends Handler<HttpClientResponse> {

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

}
