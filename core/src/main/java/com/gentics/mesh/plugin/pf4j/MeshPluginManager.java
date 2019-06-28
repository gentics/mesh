package com.gentics.mesh.plugin.pf4j;

import java.util.List;

import org.pf4j.DefaultPluginManager;
import org.pf4j.ExtensionFactory;
import org.pf4j.PluginState;

import com.gentics.mesh.plugin.ext.RestExtension;

public class MeshPluginManager extends DefaultPluginManager {

	@Override
	protected void initialize() {
		super.initialize();

		addPluginStateListener(event -> {
			List<RestExtension> ext = getExtensions(RestExtension.class, event.getPlugin().getPluginId());
			if (event.getPluginState().equals(PluginState.STARTED)) {
				ext.forEach(RestExtension::start);
			}
			if (event.getPluginState().equals(PluginState.STOPPED)) {
				ext.forEach(RestExtension::stop);
			}
		});
	}
	
	@Override
	protected ExtensionFactory createExtensionFactory() {
		return new MeshExtensionFactory();
	}

}
