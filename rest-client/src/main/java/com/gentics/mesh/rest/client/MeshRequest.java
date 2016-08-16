package com.gentics.mesh.rest.client;

public interface MeshRequest<T> {

	MeshResponse<T> invoke();

}
