package com.gentics.mesh.router;

import io.vertx.ext.web.Router;

/**
 * The root router is the top level router of the routing stack.
 */
public interface RootRouter {

	/**
	 * Return the /api/v1 router
	 * 
	 * @return
	 */
	APIRouter apiRouter();

	/**
	 * Return the Vert.x router.
	 * 
	 * @return
	 */
	Router getRouter();

	RouterStorage getStorage();

}
