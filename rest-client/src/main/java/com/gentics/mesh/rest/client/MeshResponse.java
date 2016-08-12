package com.gentics.mesh.rest.client;

import io.vertx.rxjava.core.Future;

public class MeshResponse<T> extends Future<T> {

	public MeshResponse(io.vertx.core.Future delegate) {
		super(delegate);
	}

	public static <T> MeshResponse<T> create() {
		return new MeshResponse<T>(io.vertx.core.Future.future());
	}

}
