package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

/**
 * Super basic plugin.
 */
public class DummyPlugin extends AbstractPlugin implements RestPlugin {

	public static final String API_NAME = "dummy";

	public DummyPlugin() {
	}

	public DummyPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Router createGlobalRouter() {
		Router router = Router.router(vertx());
		router.route("/hello").handler(rc -> {
			rc.response().end("world");
		});

		router.route("/manifest").handler(rc -> {
			rc.response().end(JsonUtil.toJson(getManifest()));
		});

		router.route("/id").handler(rc -> {
			rc.response().end(new JsonObject().put("id", id()).encodePrettily());
		});
		return router;
	}

	@Override
	public Router createProjectRouter() {
		Router router = Router.router(vertx());
		router.route("/hello").handler(rc -> {
			rc.response().end("project");
		});
		return router;
	}

	@Override
	public String restApiName() {
		return API_NAME;
	}

}
