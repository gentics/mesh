package com.gentics.mesh.server;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.cli.MeshImpl;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.dagger.DaggerHibernateMeshComponent;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

import io.vertx.core.json.JsonObject;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class ServerRunner {

	static {
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
		System.setProperty("org.jboss.logging.provider", "slf4j");
	}

	public static void main(String[] args) throws Exception {
		LoggingConfigurator.init();

		HibernateMeshOptions defaultOption = new HibernateMeshOptions();
		defaultOption.getSearchOptions().setUrl(null);
		HibernateMeshOptions options = OptionsLoader.createOrloadOptions(HibernateMeshOptions.class, defaultOption, args);

		Mesh mesh = new MeshImpl(options, DaggerHibernateMeshComponent.builder());
		mesh.setCustomLoader(vertx -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());
		});
		mesh.run();
	}

}
