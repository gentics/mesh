package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ExtensionProviderPlugin extends AbstractPlugin {

	private static final Logger log = LoggerFactory.getLogger(ExtensionProviderPlugin.class);

	public ExtensionProviderPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public void start() {
		log.info("Extension provider plugin started");
	}

	@Override
	public void stop() {
		log.info("Extension provider plugin stopped");
	}

}
