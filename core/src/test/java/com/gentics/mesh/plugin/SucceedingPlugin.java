package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import io.vertx.ext.web.Router;

/**
 * Plugin that will succeed in initialization
 */
public class SucceedingPlugin extends AbstractPlugin {
	
	public SucceedingPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
	}
}
