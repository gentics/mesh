package com.gentics.mesh.auth;

import io.vertx.ext.web.Route;

/**
 * Mesh OAuth Service which controls all OAuth2 specific tasks.
 */
public interface MeshOAuthService {

	/**
	 * Add the OAuth2 handlers to the given route.
	 * 
	 * @param route
	 */
	void secure(Route route);

}
