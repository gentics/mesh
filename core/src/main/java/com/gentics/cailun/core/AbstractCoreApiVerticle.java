package com.gentics.cailun.core;

import io.vertx.ext.apex.core.Router;


public abstract class AbstractCoreApiVerticle extends AbstractRestVerticle {

	protected AbstractCoreApiVerticle(String basePath) {
		super(basePath);
	}

	@Override
	public Router setupLocalRouter() {
		return routerStorage.getAPISubRouter(basePath);
	}

}
