package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

/**
 * Plugin that will fail the start
 */
public class FailingStartPlugin extends AbstractPlugin {

	public FailingStartPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public void start() {
		throw new RuntimeException("Startup failure");
	}

}
