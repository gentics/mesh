package com.gentics.mesh.auth;

import io.vertx.ext.web.Route;

/**
 * Authentication chain which processes the request by passing it along multiple nested handlers to authenticate it.
 */
public interface MeshAuthChain {

	/**
	 * Secure the given route by adding auth handlers
	 * 
	 * @param route
	 */
	void secure(Route route);

}
