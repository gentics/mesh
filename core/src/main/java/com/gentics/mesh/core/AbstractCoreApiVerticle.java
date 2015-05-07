package com.gentics.mesh.core;

import io.vertx.ext.apex.Router;

public abstract class AbstractCoreApiVerticle extends AbstractRestVerticle {

	protected AbstractCoreApiVerticle(String basePath) {
		super(basePath);
	}

	@Override
	public Router setupLocalRouter() {
		return routerStorage.getAPISubRouter(basePath);
	}

}
