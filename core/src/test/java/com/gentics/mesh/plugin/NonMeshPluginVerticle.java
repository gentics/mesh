package com.gentics.mesh.plugin;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NonMeshPluginVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(NonMeshPluginVerticle.class);

	@Override
	public void start() throws Exception {
		log.info("Starting {" + getClass().getName() + "}");
	}

	public void stop() throws Exception {
		log.info("Stopping {" + getClass().getName() + "}");
	}

}
