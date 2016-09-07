package com.gentics.mesh.search.plugins.permissions;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ScriptModule;

/**
 * Created by philippguertler on 05.09.16.
 */
public class PermissionsPlugin extends Plugin {
	@Override
	public String name() {
		return "mesh-permissions-script";
	}

	@Override
	public String description() {
		return "Filters results by user permissions";
	}

	public void onModule(ScriptModule scriptModule) {
		scriptModule.registerScript("hasPermission", PermissionsPluginFactory.class);
	}
}
