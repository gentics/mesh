package com.gentics.mesh.plugin.manager;

import org.pf4j.DefaultExtensionFinder;
import org.pf4j.PluginManager;

public class CustomDefaultExtensionFinder extends DefaultExtensionFinder {

	public CustomDefaultExtensionFinder(PluginManager pluginManager) {
		super(pluginManager);
		finders.clear();
		add(new MeshExtensionFinder(pluginManager));
	}

}
