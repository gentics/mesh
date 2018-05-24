package com.gentics.mesh.plugin;

import io.vertx.ext.web.Router;

/**
 * Plugin that will succeed in initialization
 */
public class SucceedingPlugin extends AbstractPluginVerticle {
	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
	}
}
