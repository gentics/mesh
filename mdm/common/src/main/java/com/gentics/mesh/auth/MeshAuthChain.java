package com.gentics.mesh.auth;

import io.vertx.ext.web.Route;

public interface MeshAuthChain {

	/**
	 * Secure the given route by adding auth handlers
	 * 
	 * @param route
	 */
	void secure(Route route);

}
