package com.gentics.cailun.core;

import com.gentics.cailun.etc.RouterStorage;

import io.vertx.ext.apex.core.Router;
import io.vertx.ext.apex.core.RoutingContext;

/**
 * A cailun project rest verticle is a verticle that provides rest endpoints for all registered projects. The router for this verticle will automatically be
 * mounted for all registered projects. E.g: /api/v1/yourproject/verticle_basePath
 * 
 * @author johannes2
 *
 */
public abstract class AbstractProjectRestVerticle extends AbstractCailunRestVerticle {

	protected AbstractProjectRestVerticle(String basePath) {
		super(basePath);
	}

	@Override
	public Router setupLocalRouter() {
		Router localRouter = config.routerStorage().getProjectSubRouter(basePath);
		return localRouter;
	}

	/**
	 * Extracts the project name from the routing context
	 * 
	 * @param rh
	 * @return extracted project name
	 */
	protected String getProjectName(RoutingContext rh) {
		return String.valueOf(rh.contextData().get(RouterStorage.PROJECT_CONTEXT_KEY));
	}

}
