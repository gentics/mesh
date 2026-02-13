package com.gentics.mesh.router;

import io.vertx.ext.web.Router;

/**
 * A base for all internal routers
 */
public interface InternalRouter {

	/**
	 * Internal vert.x router for the API router.
	 * 
	 * @return
	 */
	Router getRouter();
}
