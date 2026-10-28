package com.gentics.mesh.plugin.export;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;

public class ServerRunner {
	public static void main(String[] args) throws Throwable {
		File logbackFile = new File("config", "logback.xml");
		System.setProperty("logback.configurationFile", logbackFile.getAbsolutePath());

		MeshOptions options = OptionsLoader.createOrloadOptions(HibernateMeshOptions.class, args);

		options
			.setAdminPassword("admin")
			.setForceInitialAdminPasswordReset(false);

		Mesh mesh = Mesh.create(options);

		mesh.rxRun().blockingAwait(200, TimeUnit.SECONDS);
		mesh.deployPlugin(ExportPlugin.class, "export").blockingAwait(60, TimeUnit.SECONDS);
		mesh.dontExit();
	}
}