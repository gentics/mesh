package com.gentics.cailun.core;

import io.vertx.ext.apex.core.Router;

/**
 * A cailun project rest verticle is a verticle that provides rest endpoints for all registered projects. The router for this verticle will automatically be
 * mounted for all registered projects. E.g: /api/v1/yourproject/verticle_basePath
 * 
 * @author johannes2
 *
 */
public abstract class AbstractCaiLunProjectRestVerticle extends AbstractCailunRestVerticle {

	protected AbstractCaiLunProjectRestVerticle(String basePath) {
		super(basePath);
	}

	@Override
	public Router setupLocalRouter() {

		Router localRouter = config.routerStorage().getProjectSubRouter(basePath);
		return localRouter;

	}

}
