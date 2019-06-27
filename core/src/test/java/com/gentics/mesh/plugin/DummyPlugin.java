package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.json.JsonUtil;

import io.vertx.ext.web.Router;

/**
 * Super basic plugin.
 */
public class DummyPlugin extends AbstractPlugin {

	public DummyPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
		globalRouter.route("/hello").handler(rc -> {
			rc.response().end("world");
		});

		projectRouter.route("/hello").handler(rc -> {
			rc.response().end("project");
		});

		globalRouter.route("/manifest").handler(rc -> {
			rc.response().end(JsonUtil.toJson(getManifest()));
		});
	}

}
