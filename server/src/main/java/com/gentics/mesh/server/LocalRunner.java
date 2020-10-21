package com.gentics.mesh.server;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.verticle.admin.AdminGUIEndpoint;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class LocalRunner {

	static {
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
	}

	public static void main(String[] args) throws Exception {
		LoggingConfigurator.init();
		Logger log = LoggerFactory.getLogger(LocalRunner.class);

		MeshOptions options = OptionsLoader.createOrloadOptions(args);

		// options.setAdminPassword("admin");
		// options.getStorageOptions().setStartServer(true);
		// options.getHttpServerOptions().setCorsAllowCredentials(true);
		// options.getHttpServerOptions().setEnableCors(true);
		// options.getHttpServerOptions().setCorsAllowedOriginPattern("http://localhost:5000");
		options.getAuthenticationOptions().setKeystorePassword("finger");
		options.getStorageOptions().setStartServer(true);
		options.getSearchOptions().setUrl(null);
		options.getSearchOptions().setStartEmbedded(false);

		Mesh mesh = Mesh.create(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());

			// Add admin ui
			MeshComponent meshInternal = mesh.internal();
			EndpointRegistry registry = meshInternal.endpointRegistry();
			registry.register(AdminGUIEndpoint.class);

		});

		try {
			mesh.run();
		} catch (Throwable t) {
			log.error("Error while starting mesh. Invoking shutdown.", t);
			mesh.shutdownAndTerminate(10);
		}
	}

}
