package com.gentics.mesh.core;

import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;

import io.vertx.ext.web.Router;

public abstract class AbstractCustomVerticle extends AbstractWebVerticle {

	protected AbstractCustomVerticle(String basePath, RouterStorage routerStorage, MeshSpringConfiguration springConfig) {
		super(basePath, routerStorage, springConfig);
	}

	@Override
	public Router setupLocalRouter() {
		Router localRouter = routerStorage.getCustomSubRouter(basePath);
		return localRouter;
	}
}
