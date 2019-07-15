package com.gentics.mesh.plugin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NonMeshPlugin extends Plugin {

	private static final Logger log = LoggerFactory.getLogger(NonMeshPlugin.class);

	public NonMeshPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Override
	public void start() {
		log.info("Starting {" + getClass().getName() + "}");
	}

	public void stop() {
		log.info("Stopping {" + getClass().getName() + "}");
	}
}
