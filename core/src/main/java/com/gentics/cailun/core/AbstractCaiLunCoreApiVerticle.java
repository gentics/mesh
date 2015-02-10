package com.gentics.cailun.core;

import io.vertx.ext.apex.core.Router;

public abstract class AbstractCaiLunCoreApiVerticle extends AbstractCailunRestVerticle {

	protected AbstractCaiLunCoreApiVerticle(String basePath) {
		super(basePath);
	}

	@Override
	public Router setupLocalRouter() {
		return config.routerStorage().getAPISubRouter(basePath);
	}

}
