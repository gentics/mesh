package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class Basic2Plugin extends AbstractPlugin implements RestPlugin {

	private static final Logger log = LoggerFactory.getLogger(Basic2Plugin.class);

	public Basic2Plugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Router createGlobalRouter() {
		log.info("Creating routes for {" + name() + "}");

		Router router = Router.router(vertx());
		router.route("/hello").handler(rc -> {
			rc.response().end("world2");
		});

		StaticHandler staticHandler = StaticHandler.create("webroot-basic2", getClass().getClassLoader());
		router.route("/static2/*").handler(staticHandler);

		return router;
	}

	@Override
	public Router createProjectRouter() {
		log.info("Creating routes for {" + name() + "}");

		Router router = Router.router(vertx());
		router.route("/hello").handler(rc -> {
			rc.response().end("world2-project");
		});
		return router;
	}

	@Override
	public String restApiName() {
		return "basic2";
	}

}
