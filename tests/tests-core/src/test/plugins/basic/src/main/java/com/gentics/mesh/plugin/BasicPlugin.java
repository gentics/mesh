package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.Router;

public class BasicPlugin extends AbstractPlugin implements RestPlugin {

	private static final Logger log = LoggerFactory.getLogger(BasicPlugin.class);

	public BasicPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Router createGlobalRouter() {
		log.info("Registering routes for {" + name() + "}");
		Router router = Router.router(vertx());

		router.route("/hello").handler(rc -> {
			rc.response().end("world");
		});

		addStaticHandlerFromClasspath(router.route("/static/*"), "webroot-basic");

		return router;
	}

	@Override
	public Router createProjectRouter() {
		Router router = Router.router(vertx());

		router.route("/hello").handler(rc -> {
			rc.response().end("world-project");
		});

		router.route("/projectInfo").handler(rc -> {
			JsonObject info = (JsonObject) rc.data().get("mesh.project");
			rc.response().end(info.encodePrettily());
		});

		return router;
	}

	@Override
	public String restApiName() {
		return "basic";
	}

}
