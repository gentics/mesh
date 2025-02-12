package com.gentics.mesh.plugin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
