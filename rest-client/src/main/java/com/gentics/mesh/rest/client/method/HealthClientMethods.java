package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

public interface HealthClientMethods {
	/**
	 * Invoke a readiness probe request.
	 *
	 * @return
	 */
	MeshRequest<EmptyResponse> ready();

	/**
	 * Invoke a live probe request.
	 *
	 * @return
	 */
	MeshRequest<EmptyResponse> live();

	/**
	 * Invoke a writable request.
	 *
	 * @return
	 */
	MeshRequest<EmptyResponse> writable();
}
