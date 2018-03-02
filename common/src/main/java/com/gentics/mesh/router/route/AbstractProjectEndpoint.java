package com.gentics.mesh.router.route;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.router.RouterStorage;

/**
 * A endpoint which provides more REST endpoints for all registered projects. The router for this endpoint will automatically be mounted for all registered
 * projects. E.g: /api/v1/yourproject/endpoint_basePath
 */
public abstract class AbstractProjectEndpoint extends AbstractInternalEndpoint {

	protected BootstrapInitializer boot;

	protected AbstractProjectEndpoint(String basePath, BootstrapInitializer boot) {
		super(basePath);
		this.boot = boot;
	}

	@Override
	public void init(RouterStorage rs) {
		localRouter = rs.root().apiRouter().projectsRouter().projectRouter().getOrCreate(basePath);
	}

}
