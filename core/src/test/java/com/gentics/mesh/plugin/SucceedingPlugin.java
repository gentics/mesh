package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

/**
 * Plugin that will succeed in initialization
 */
public class SucceedingPlugin extends AbstractPlugin {

	public SucceedingPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

}
