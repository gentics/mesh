package com.gentics.mesh.rest.client.impl;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;

public class MeshLocalRequestImpl<T> implements MeshRequest<T> {

	private Future<T> future;

	public MeshLocalRequestImpl(Future<T> future) {
		this.future = future;
	}

	@Override
	public MeshResponse<T> invoke() {
		return new MeshResponse<>(future);
	}

	@Override
	public HttpClientRequest getRequest() {
		throw new NotImplementedException("The Http request object can't be used for local requests.");
	}

}
