package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;

public class AuthPlugin extends AbstractPlugin implements AuthServicePlugin {
	public AuthPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}
}
