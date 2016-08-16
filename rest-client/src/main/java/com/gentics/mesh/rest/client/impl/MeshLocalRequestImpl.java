package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;

import io.vertx.core.Future;

public class MeshLocalRequestImpl<T> implements MeshRequest<T> {

	private Future<T> future;

	public MeshLocalRequestImpl(Future<T> future) {
		this.future = future;
	}

	@Override
	public MeshResponse<T> invoke() {
		return new MeshResponse<>(future);
	}

}
