package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

public class StaticPlugin extends AbstractPlugin implements RestPlugin {

	private static final Logger log = LoggerFactory.getLogger(StaticPlugin.class);

	public StaticPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		return super.initialize();
	}

	@Override
	public Router createGlobalRouter() {
		log.info("Registering routes for {" + name() + "}");
		Router router = Router.router(vertx());

		addStaticHandlerFromClasspath(router.route("/static/*"), "webroot-basic");

		return router;
	}
}
