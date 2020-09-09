package com.gentics.mesh.router;

import io.vertx.ext.web.Router;

/**
 * The root router is the top level router of the routing stack.
 */
public interface RootRouter {

	APIRouter apiRouter();

	Router getRouter();

	RouterStorage getStorage();

}
