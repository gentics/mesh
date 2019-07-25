package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

public class ClassloaderPlugin extends AbstractPlugin implements RestPlugin {

	private static final Logger log = LoggerFactory.getLogger(ClassloaderPlugin.class);

	public ClassloaderPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
		log.info("Registering routes for {" + name() + "}");

		globalRouter.route("/scope").handler(rc -> {
			rc.response().end(ConflictingClass.scope);
		});

		globalRouter.route("/check").handler(rc -> {
			rc.response().end(ConflictingClass.check());
		});
	}

	@Override
	public String apiName() {
		return "classloader";
	}

}
