package com.gentics.mesh.core;

import com.gentics.mesh.etc.RouterStorage;

import io.vertx.ext.web.Router;

/**
 * Abstract implementation for custom verticles (e.g.: Demo Verticle, AdminUI Verticle)
 */
public abstract class AbstractCustomVerticle extends AbstractEndpoint {

	protected AbstractCustomVerticle(String basePath, RouterStorage routerStorage) {
		super(basePath, routerStorage);
	}

	@Override
	public Router setupLocalRouter() {
		Router localRouter = routerStorage.getCustomSubRouter(basePath);
		return localRouter;
	}
}
