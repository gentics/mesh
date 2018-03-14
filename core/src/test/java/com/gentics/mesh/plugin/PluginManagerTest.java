package com.gentics.mesh.plugin;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;

public class PluginManagerTest {

	@Test
	public void testManager() throws IOException {
		PluginManagerImpl manager = new PluginManagerImpl();
		manager.init(new MeshOptions());
		System.out.println("Done");
		System.in.read();
	}
}
