package com.gentics.mesh.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
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
	
	@Before
	public void cleanConfigFiles() {		
		DummyPlugin plugin = new DummyPlugin();
		plugin.getConfigFile().delete();
		plugin.getLocalConfigFile().delete();
	}

	@Test
	public void testMissingConfig() throws Exception {
		DummyPlugin plugin = new DummyPlugin();
		assertNull(plugin.readConfig(DummyPluginConfig.class));
	}

	@Test
	public void testWriteConfig() throws Exception {

		DummyPlugin plugin = new DummyPlugin();

		DummyPluginConfig config = new DummyPluginConfig();
		config.setName("test");

		plugin.writeConfig(config);
		assertTrue(plugin.getConfigFile().exists());
		assertEquals("test", plugin.readConfig(DummyPluginConfig.class).getName());
		assertEquals(PLUGIN_DIR + "/dummy/config.yml", plugin.getConfigFile().getPath());
		assertEquals(PLUGIN_DIR + "/dummy/storage", plugin.getStorageDir().getPath());
	}
	

	@Test
	public void testReadConfigOverride() throws FileNotFoundException, IOException {
		DummyPlugin plugin = new DummyPlugin();
		
		DummyPluginConfig config = new DummyPluginConfig();
		config.setName("local");
		
		plugin.writeConfig(config);
		assertTrue(plugin.getConfigFile().exists());
		
		FileUtils.copyFile(plugin.getConfigFile(), plugin.getLocalConfigFile());
		assertTrue(plugin.getLocalConfigFile().exists());
		
		
		config.setName("original");
		plugin.writeConfig(config);
		assertTrue(plugin.getConfigFile().exists());
		
		config = plugin.readConfig(DummyPluginConfig.class);
		assertEquals("local", config.getName());

	}
}
