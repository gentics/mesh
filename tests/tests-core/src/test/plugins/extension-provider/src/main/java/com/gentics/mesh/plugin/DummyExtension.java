package com.gentics.mesh.plugin;

import org.pf4j.Extension;

import com.gentics.mesh.plugin.common.DummyExtensionPoint;

@Extension
public class DummyExtension implements DummyExtensionPoint {

	@Override
	public String name() {
		return "My dummy extension";
	}

}
