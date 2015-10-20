package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

public interface AdminClientMethods {

	/**
	 * Return the mesh status.
	 * 
	 * @return
	 */
	Future<String> getMeshStatus();

}
