package com.gentics.mesh.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;

public class PluginConfigTest {

	public static String PLUGIN_DIR = "target/plugins" + System.currentTimeMillis();

	@BeforeClass
	public static void setupMeshOptions() {
		MeshOptions options = new MeshOptions();
		options.setPluginDirectory(PLUGIN_DIR);
		Mesh.mesh(options);
	}

	@Test
	public void testMissingConfig() throws Exception {
		DummyPlugin plugin = new DummyPlugin();
		plugin.getConfigFile().delete();
		assertNull(plugin.readConfig(DummyPluginConfig.class));
	}

	@Test
	public void testWriteConfig() throws Exception {

		DummyPlugin plugin = new DummyPlugin();
		plugin.getConfigFile().delete();

		DummyPluginConfig config = new DummyPluginConfig();
		config.setName("test");

		plugin.writeConfig(config);
		assertTrue(plugin.getConfigFile().exists());
		assertEquals("test", plugin.readConfig(DummyPluginConfig.class).getName());
		assertEquals(PLUGIN_DIR + "/dummy/config.yml", plugin.getConfigFile().getPath());
		assertEquals(PLUGIN_DIR + "/dummy/storage", plugin.getStorageDir().getPath());
	}
}
