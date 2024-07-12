package com.gentics.mesh.plugin.export;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.impl.HibernateMeshFactoryImpl;

public class ExportPluginInitializationTest {

	private static Mesh mesh = null;

	@BeforeClass
	public static void init() {
		File logbackFile = new File("config", "logback.xml");
		System.setProperty("logback.configurationFile", logbackFile.getAbsolutePath());
		HibernateMeshOptions options = OptionsLoader.createOrloadOptions(HibernateMeshOptions.class);;

		options
			.setAdminPassword("admin")
			.setForceInitialAdminPasswordReset(false);

		mesh = new HibernateMeshFactoryImpl().create(options);

		mesh.rxRun().blockingAwait(200, TimeUnit.SECONDS);
	}

	@AfterClass
	public static void deinit() throws Exception {
		mesh.shutdown();
		//FileUtils.deleteDirectory(Path.of("").toAbsolutePath().resolve("data").toFile());
	}

	@Test
	public void testInitialization() {
		try {
			mesh.deployPlugin(ExportPlugin.class, "export").blockingAwait(60, TimeUnit.SECONDS);
			assertEquals(mesh.<MeshComponent>internal().pluginManager().getStatus("export"), PluginStatus.REGISTERED);
		} finally {
			mesh.<MeshComponent>internal().pluginManager().undeploy("export");
		}
	}
}
