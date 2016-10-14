package com.gentics.mesh.core;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.etc.RouterStorage;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

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

	/**
	 * Load the project with the name that could be extracted from the routing context.
	 * 
	 * @param rc
	 * @return
	 */
	public Project getProject(RoutingContext rc) {
		return boot.projectRoot().findByName(getProjectName(rc));
	}

	/**
	 * Return the project name which was stored in the routing context data during route resolving.
	 * 
	 * @param rc
	 * @return
	 */
	public String getProjectName(RoutingContext rc) {
		return rc.get(RouterStorage.PROJECT_CONTEXT_KEY);
	}

}
