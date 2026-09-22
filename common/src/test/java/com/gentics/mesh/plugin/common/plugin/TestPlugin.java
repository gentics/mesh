package com.gentics.mesh.plugin.common.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.CommercialRestPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;

/**
 * Simple test plugin
 */
public class TestPlugin extends AbstractPlugin implements CommercialRestPlugin {
	/**
	 * Create an instance
	 * @param wrapper plugin wrapper
	 * @param env environment
	 */
	public TestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}
}
