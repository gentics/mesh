package com.gentics.mesh.plugin;

import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;

public class PluginManagerTest {

	@Test
	public void testManager() {
		PluginManager manager = new PluginManager();
		manager.init(new MeshOptions());
		System.out.println("Done");
	}
}
