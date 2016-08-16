package com.gentics.mesh.rest.client;

import io.vertx.core.http.HttpClientRequest;

public interface MeshRequest<T> {

	/**
	 * Invoke the request and return the async response.
	 * 
	 * @return
	 */
	MeshResponse<T> invoke();

	/**
	 * Return the raw request. The request can be altered before {@link #invoke()} is called.
	 * 
	 * @return
	 */
	HttpClientRequest getRequest();
}
