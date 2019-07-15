package com.gentics.mesh.plugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.test.context.AbstractMeshTest;

/**
 * Dedicated abstract class which takes care of setting an independent plugin directory for each test.
 */
public class AbstractPluginTest extends AbstractMeshTest {

	public static final String PLUGIN_DIR = "target/plugins" + System.currentTimeMillis();

	@Before
	public void preparePluginDir() throws IOException {
		MeshPluginManager manager = pluginManager();
		manager.stop().blockingAwait(15, TimeUnit.SECONDS);
		setPluginBaseDir(PLUGIN_DIR);
	}

	@AfterClass
	public static void cleanup() throws IOException {
		File dir = new File(PLUGIN_DIR);
		if (dir.exists()) {
			FileUtils.forceDelete(dir);
		}
	}

	protected void setPluginBaseDir(String baseDir) {
		File pluginDir = new File(baseDir);
		pluginDir.mkdirs();
		MeshOptions options = new MeshOptions();
		options.setPluginDirectory(baseDir);
		pluginManager().init(options);
	}

}
