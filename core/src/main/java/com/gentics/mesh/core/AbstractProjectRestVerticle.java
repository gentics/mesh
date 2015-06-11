package com.gentics.mesh.core;

import io.vertx.ext.apex.Router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.verticle.MeshNodeVerticle;

/**
 * A mesh project rest verticle is a verticle that provides rest endpoints for all registered projects. The router for this verticle will automatically be
 * mounted for all registered projects. E.g: /api/v1/yourproject/verticle_basePath
 * 
 * @author johannes2
 *
 */
public abstract class AbstractProjectRestVerticle extends AbstractRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(MeshNodeVerticle.class);

	protected AbstractProjectRestVerticle(String basePath) {
		super(basePath);
	}

	@Override
	public Router setupLocalRouter() {
		Router localRouter = routerStorage.getProjectSubRouter(basePath);
		return localRouter;
	}

}
