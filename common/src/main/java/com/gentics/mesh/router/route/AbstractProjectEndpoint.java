package com.gentics.mesh.router.route;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.RouterStorage;

import io.vertx.core.Vertx;

/**
 * A endpoint which provides more REST endpoints for all registered projects. The router for this endpoint will automatically be mounted for all registered
 * projects. E.g: :apibase:/yourproject/endpoint_basePath
 */
public abstract class AbstractProjectEndpoint extends AbstractInternalEndpoint {

	protected BootstrapInitializer boot;

	protected AbstractProjectEndpoint(String basePath, MeshAuthChain chain, BootstrapInitializer boot, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super(basePath, chain, localConfigApi, db, options);
		this.boot = boot;
	}

	@Override
	public void init(Vertx vertx, RouterStorage rs) {
		localRouter = rs.root().apiRouter().projectsRouter().projectRouter().getOrCreate(basePath);
	}

}
