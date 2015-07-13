package com.gentics.mesh.core;

import io.vertx.ext.web.Router;

public abstract class AbstractCustomVerticle extends AbstractWebVerticle {

	protected AbstractCustomVerticle(String basePath) {
		super(basePath);
	}

	@Override
	public Router setupLocalRouter() {
		Router localRouter = routerStorage.getCustomSubRouter(basePath);
		return localRouter;
	}
}
