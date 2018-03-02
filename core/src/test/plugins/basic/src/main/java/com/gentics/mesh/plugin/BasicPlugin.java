package com.gentics.mesh.plugin;

import com.gentics.mesh.plugin.AbstractPluginVerticle;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class BasicPlugin extends AbstractPluginVerticle {

	private static final Logger log = LoggerFactory.getLogger(BasicPlugin.class);

	public StaticHandler staticHandler = StaticHandler.create("webroot", getClass().getClassLoader());

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
		log.info("Registering routes for {" + getName() + "}");

		globalRouter.route("/hello").handler(rc -> {
			rc.response().end("world");
		});

		projectRouter.route("/hello").handler(rc -> {
			rc.response().end("world-project");
		});

		globalRouter.route("/static/*").handler(staticHandler);

	}

}
