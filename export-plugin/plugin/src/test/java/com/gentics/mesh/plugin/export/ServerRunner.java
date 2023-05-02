package com.gentics.mesh.plugin.export;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class ServerRunner {
	public static void main(String[] args) throws Throwable {
		File logbackFile = new File("config", "logback.xml");
		System.setProperty("logback.configurationFile", logbackFile.getAbsolutePath());
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		LoggerFactory.getLogger(LoggingConfigurator.class);

		MeshOptions options = OptionsLoader.createOrloadOptions(OrientDBMeshOptions.class, args);

		options
			.setAdminPassword("admin")
			.setForceInitialAdminPasswordReset(false);

		Mesh mesh = Mesh.create(options);

		mesh.rxRun().blockingAwait(200, TimeUnit.SECONDS);
		mesh.deployPlugin(ExportPlugin.class, "export").blockingAwait(60, TimeUnit.SECONDS);
		mesh.dontExit();
	}
}