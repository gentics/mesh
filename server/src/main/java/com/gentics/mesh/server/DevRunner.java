package com.gentics.mesh.server;


import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.cli.MeshImpl;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.dagger.DaggerHibernateMeshComponent;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.impl.DefaultJwtParserBuilder;
import io.vertx.core.json.JsonObject;


/**
 * Main runner that is used to deploy a preconfigured set of verticles. Sets the default data for development purposes, if started over a clean database.
 */
public class DevRunner {

	public static boolean DEV_MODE_ON = false;

	static {
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
		System.setProperty("org.jboss.logging.provider", "slf4j");

		// TODO FIXME These dependencies make mess on the commercial plugins, so required to be explicitly loaded.
		JwtParserBuilder builder = new DefaultJwtParserBuilder();
		builder.build();
	}

	public static void main(String[] args) throws Exception {
		LoggingConfigurator.init();
		HibernateMeshOptions defaultOption = new HibernateMeshOptions();
		defaultOption.getSearchOptions().setUrl(null);
		defaultOption.setInitialAdminPassword("admin");
		defaultOption.setForceInitialAdminPasswordReset(false);
		defaultOption.getDebugInfoOptions().setLogEnabled(false);

		HibernateMeshOptions options = OptionsLoader.createOrloadOptions(HibernateMeshOptions.class, defaultOption, args);

		Mesh mesh = new MeshImpl(options, DaggerHibernateMeshComponent.builder());
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());
		});
		mesh.run();
	}

}
