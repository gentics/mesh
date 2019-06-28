package com.gentics.mesh.plugin;

import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.ext.AbstractRestExtension;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class BasicPlugin extends AbstractPlugin {

	private static final Logger log = LoggerFactory.getLogger(BasicPlugin.class);

	public BasicPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Extension
	public class BasicRestExtension extends AbstractRestExtension {

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

}
