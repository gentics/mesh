package com.gentics.mesh.core;

import io.vertx.ext.web.Router;

public abstract class AbstractCoreApiVerticle extends AbstractWebVerticle {

	protected AbstractCoreApiVerticle(String basePath) {
		super(basePath);
	}

	@Override
	public Router setupLocalRouter() {
		return routerStorage.getAPISubRouter(basePath);
	}

}
