package com.gentics.mesh.rest.client;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;

public class MeshRequest<T> {

	private HttpClientRequest request;

	private MeshResponse<T> future;
	
	public MeshRequest(HttpClientRequest request, Future<T> future) {
		this.request = request;
		this.future = new MeshResponse<>(future);
	}

	public MeshRequest(HttpClientRequest request, MeshResponse<T> future) {
		this.request = request;
		this.future = future;
	}

	public HttpClientRequest getRequest() {
		return request;
	}

	public MeshResponse<T> invoke() {
		request.end();
		return future;
	}
}
