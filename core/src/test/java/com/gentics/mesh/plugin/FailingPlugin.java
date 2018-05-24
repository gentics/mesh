package com.gentics.mesh.plugin;

import io.reactivex.Completable;
import io.vertx.ext.web.Router;

/**
 * Plugin that will fail the initialization
 */
public class FailingPlugin extends AbstractPluginVerticle {
	@Override
	public Completable initialize() {
		return Completable.error(new Exception("This must fail"));
	}

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
	}
}
