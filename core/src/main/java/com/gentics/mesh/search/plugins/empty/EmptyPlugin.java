package com.gentics.mesh.search.plugins.empty;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ScriptModule;

/**
 * Created by philippguertler on 05.09.16.
 */
public class EmptyPlugin extends Plugin {
	@Override
	public String name() {
		return "mesh-empty-script";
	}

	@Override
	public String description() {
		return "Dummy Script to test performance";
	}

	public void onModule(ScriptModule scriptModule) {
		scriptModule.registerScript("empty", EmptyPluginFactory.class);
	}
}
