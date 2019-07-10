package com.gentics.mesh.plugin.pf4j;

import org.pf4j.DefaultPluginDescriptor;
import org.pf4j.MeshPluginManagerImpl;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class LocalPluginWrapper extends PluginWrapper {

	Plugin plugin;

	public LocalPluginWrapper(Plugin plugin, MeshPluginManagerImpl meshPluginManager, ClassLoader classLoader) {
		super(meshPluginManager, new DefaultPluginDescriptor("plugin-id", "desc", null, "version", null, null,  null), null, classLoader);
		this.plugin = plugin;
	}

	@Override
	public Plugin getPlugin() {
		return plugin;
	}

}
