package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.common.DummyExtensionPoint;
import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

public class ExtensionConsumerPlugin extends AbstractPlugin implements RestPlugin {

	private static final Logger log = LoggerFactory.getLogger(ExtensionConsumerPlugin.class);

	public ExtensionConsumerPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
		log.info("Registering routes for {" + name() + "}");

		globalRouter.route("/extensions").handler(rc -> {
			StringBuilder builder = new StringBuilder();
			getWrapper().getPluginManager().getExtensions(DummyExtensionPoint.class).stream().map(e -> e.name()).forEach(name -> {
				builder.append(name + "\n");
			});
			rc.response().end(builder.toString());
		});

	}

}
