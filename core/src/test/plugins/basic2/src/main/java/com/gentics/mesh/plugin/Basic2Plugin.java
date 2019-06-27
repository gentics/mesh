package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class Basic2Plugin extends AbstractPlugin {

	public Basic2Plugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	private static final Logger log = LoggerFactory.getLogger(Basic2Plugin.class);

	public StaticHandler staticHandler = StaticHandler.create("webroot", getClass().getClassLoader());

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
		log.info("Registering routes for {" + getName() + "}");

		globalRouter.route("/hello").handler(rc -> {
			rc.response().end("world2");
		});

		projectRouter.route("/hello").handler(rc -> {
			rc.response().end("world2-project");
		});

		globalRouter.route("/static/*").handler(staticHandler);

	}

}
