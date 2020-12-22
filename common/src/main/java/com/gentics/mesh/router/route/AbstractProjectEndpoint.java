package com.gentics.mesh.router.route;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.router.RouterStorage;

import io.vertx.core.Vertx;

/**
 * A endpoint which provides more REST endpoints for all registered projects. The router for this endpoint will automatically be mounted for all registered
 * projects. E.g: :apibase:/yourproject/endpoint_basePath
 */
public abstract class AbstractProjectEndpoint extends AbstractInternalEndpoint {

	protected BootstrapInitializer boot;

	protected AbstractProjectEndpoint(String basePath, MeshAuthChainImpl chain, BootstrapInitializer boot) {
		super(basePath, chain);
		this.boot = boot;
	}

	@Override
	public void init(Vertx vertx, RouterStorage rs) {
		localRouter = rs.root().apiRouter().projectsRouter().projectRouter().getOrCreate(basePath);
	}

}
