package com.gentics.mesh.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.impl.MeshPluginDescriptorImpl;

public class PluginConfigTest {

	public static String PLUGIN_DIR = "target/plugins" + System.currentTimeMillis();

	@BeforeClass
	public static void setupMeshOptions() {
		MeshOptions options = new MeshOptions();
		options.setNodeName("ABC");
		options.getAuthenticationOptions().setKeystorePassword("ABC");
		options.setPluginDirectory(PLUGIN_DIR);
		Mesh.mesh(options);
	}

	@Before
	public void cleanConfigFiles() {
		DummyPlugin plugin = mockPlugin();
		plugin.getConfigFile().delete();
		plugin.getLocalConfigFile().delete();
	}

	@Test
	public void testMissingConfig() throws Exception {
		DummyPlugin plugin = mockPlugin();
		assertNull(plugin.readConfig(DummyPluginConfig.class));
	}

	@Test
	public void testWriteConfig() throws Exception {
		DummyPlugin plugin = mockPlugin();
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
		DummyPlugin plugin = mockPlugin();
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

	private DummyPlugin mockPlugin() {
		PluginWrapper wrapper = mock(PluginWrapper.class);
		when(wrapper.getPluginId()).thenReturn("dummy");

		MeshPluginDescriptorImpl descriptor = mock(MeshPluginDescriptorImpl.class);
		when(descriptor.getName()).thenReturn("dummy");

		PluginManifest manifest = new PluginManifest();
		manifest.setName("dummy");
		when(descriptor.toPluginManifest()).thenReturn(manifest);
		when(wrapper.getDescriptor()).thenReturn(descriptor);
		PluginEnvironment env = mock(PluginEnvironment.class);
		DummyPlugin plugin = new DummyPlugin(wrapper, env);
		return plugin;
	}
}
