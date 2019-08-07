package com.gentics.mesh.auth;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;

public class MapperTestPlugin extends AbstractPlugin implements AuthServicePlugin {

	public MapperTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

}
