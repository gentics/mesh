package com.gentics.mesh.core;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.etc.RouterStorage;

import io.vertx.ext.web.Router;

/**
 * A endpoint which provides more REST endpoints for all registered projects. The router for this endpoint will automatically be mounted for all registered
 * projects. E.g: /api/v1/yourproject/endpoint_basePath
 */
public abstract class AbstractProjectEndpoint extends AbstractEndpoint {

	protected BootstrapInitializer boot;

	protected AbstractProjectEndpoint(String basePath, BootstrapInitializer boot, RouterStorage routerStorage) {
		super(basePath, routerStorage);
		this.boot = boot;
	}

	@Override
	public Router setupLocalRouter() {
		Router localRouter = routerStorage.getProjectSubRouter(basePath);
		return localRouter;
	}

}
