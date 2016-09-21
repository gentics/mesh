package com.gentics.mesh.rest.client;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.rxjava.core.Future;

public class MeshResponse<T> extends Future<T> {

	private HttpClientResponse response;

	public MeshResponse(io.vertx.core.Future<T> delegate) {
		super(delegate);
	}

	public static <T> MeshResponse<T> create() {
		return new MeshResponse<T>(io.vertx.core.Future.future());
	}

	/**
	 * Get the raw response.
	 * 
	 * @return
	 */
	public HttpClientResponse getResponse() {
		return response;
	}

	/**
	 * Set the raw response.
	 * 
	 * @param response
	 */
	public void setResponse(HttpClientResponse response) {
		this.response = response;
	}

}
