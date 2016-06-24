package com.gentics.mesh.core;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.etc.RouterStorage;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * A verticle which provides REST endpoints for all registered projects. The router for this verticle will automatically be mounted for all registered projects.
 * E.g: /api/v1/yourproject/verticle_basePath
 */
public abstract class AbstractProjectRestVerticle extends AbstractWebVerticle {

	protected AbstractProjectRestVerticle(String basePath) {
		super(basePath);
	}

	@Override
	public Router setupLocalRouter() {
		Router localRouter = routerStorage.getProjectSubRouter(basePath);
		return localRouter;
	}

	public Project getProject(RoutingContext rc) {
		return boot.projectRoot().findByName(getProjectName(rc)).toBlocking().single();
	}

	public String getProjectName(RoutingContext rc) {
		return rc.get(RouterStorage.PROJECT_CONTEXT_KEY);
	}

}
