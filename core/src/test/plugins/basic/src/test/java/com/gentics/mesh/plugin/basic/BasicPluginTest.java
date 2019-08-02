package com.gentics.mesh.plugin.basic;

import static org.junit.Assert.assertEquals;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.plugin.BasicPlugin;
import com.gentics.mesh.test.local.MeshLocalServer;

public class BasicPluginTest {

	@ClassRule
	public static final MeshLocalServer server = new MeshLocalServer()
		.withInMemoryMode()
		.withPlugin(BasicPlugin.class, "basic")
		.waitForStartup();

	@Test
	public void testPlugin() {
		PluginResponse plugin = server.client().findPlugin("basic").blockingGet();
		assertEquals("basic", plugin.getId());
	}
}
