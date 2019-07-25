package com.gentics.mesh.plugin.basic;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.plugin.ClassloaderPlugin;
import com.gentics.mesh.test.local.MeshLocalServer;

public class BasicPluginTest {

	@ClassRule
	public static final MeshLocalServer server = new MeshLocalServer()
		.withInMemoryMode()
		.waitForStartup();

	@Test
	public void testPlugin() {
		Mesh mesh = server.getMesh();
		mesh.deployPlugin(ClassloaderPlugin.class, "basic");
	}
}
