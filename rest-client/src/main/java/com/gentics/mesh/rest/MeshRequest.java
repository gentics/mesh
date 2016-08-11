package com.gentics.mesh.rest;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;

public class MeshRequest<T> {

	private HttpClientRequest request;

	private MeshResponseHandler<T> handler;
	private Future<T> future;

	public MeshRequest(HttpClientRequest request, MeshResponseHandler<T> handler) {
		this.request = request;
		this.handler = handler;
	}

	public MeshRequest(HttpClientRequest request, Future<T> future) {
		this.request = request;
		this.future = future;
	}

	public HttpClientRequest getRequest() {
		return request;
	}

	public MeshResponseHandler<T> getHandler() {
		return handler;
	}

	public Future<T> invoke() {
		request.end();
		if (handler != null) {
			return handler.getFuture();
		} else {
			return future;
		}
	}
}
