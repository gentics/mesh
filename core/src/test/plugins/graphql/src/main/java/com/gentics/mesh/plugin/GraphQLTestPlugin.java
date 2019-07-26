package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GraphQLTestPlugin extends AbstractPlugin implements GraphQLPlugin {

	private static final Logger log = LoggerFactory.getLogger(GraphQLTestPlugin.class);

	public GraphQLTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public void start() {
		log.info("Starting GraphQL plugin");
	}
}
