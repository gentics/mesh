package com.gentics.mesh.core;

import com.gentics.mesh.etc.RouterStorage;

import io.vertx.ext.web.Router;

public abstract class AbstractCoreApiVerticle extends AbstractWebVerticle {

	protected AbstractCoreApiVerticle(String basePath, RouterStorage routerStorage) {
		super(basePath, routerStorage);
	}

	@Override
	public Router setupLocalRouter() {
		return routerStorage.getAPISubRouter(basePath);
	}

}
