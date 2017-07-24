package com.gentics.mesh.test.context;

import com.gentics.mesh.rest.client.MeshRequest;

@FunctionalInterface
public interface ClientHandler<T> {
	MeshRequest<T> handle() throws Exception;
}
