package com.gentics.mesh.server;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.verticle.admin.AdminGUI2Endpoint;
import com.gentics.mesh.verticle.admin.AdminGUIEndpoint;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class DevRunner {

	static {
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
		if ("jotschi".equalsIgnoreCase(System.getProperty("user.name"))) {
			System.setProperty("storage.wal.allowDirectIO", "false");
		}
	}

	/**
	 * Start the dev runner.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		LoggingConfigurator.init();
		Logger log = LoggerFactory.getLogger(DevRunner.class);

		MeshOptions defaultOption = new MeshOptions();
		defaultOption.getSearchOptions().setStartEmbedded(false);
		defaultOption.getSearchOptions().setUrl(null);
		defaultOption.setInitialAdminPassword("admin");
		defaultOption.setForceInitialAdminPasswordReset(false);
		defaultOption.getDebugInfoOptions().setLogEnabled(false);
		MeshOptions options = OptionsLoader.createOrloadOptions(defaultOption, args);

		// options.setAdminPassword("admin");
		// options.getStorageOptions().setStartServer(true);
		// options.getHttpServerOptions().setCorsAllowCredentials(true);
		// options.getHttpServerOptions().setEnableCors(true);
		// options.getHttpServerOptions().setCorsAllowedOriginPattern("http://localhost:5000");
		// options.setInitCluster(true);
		// options.getAuthenticationOptions().setKeystorePassword("finger");
		// options.getStorageOptions().setStartServer(true);
		// options.getClusterOptions().setVertxPort(6152);
		// options.getClusterOptions().setClusterName("test");
		// options.setNodeName("node1");
		// options.getClusterOptions().setEnabled(true);
		// options.getHttpServerOptions().setPort(9999);
		// options.getMonitoringOptions().setPort(9991);
		// options.getMonitoringOptions().setHost("0.0.0.0");
		// options.getSearchOptions().setUrl(null);
		// options.getSearchOptions().setStartEmbedded(false);
		// options.getSearchOptions().setMappingMode(MappingMode.STRICT);
		options.getSearchOptions().disable();

		Mesh mesh = Mesh.create(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());

			// Add admin ui
			MeshComponent meshInternal = mesh.internal();
			EndpointRegistry registry = meshInternal.endpointRegistry();
			registry.register(AdminGUIEndpoint.class);
			registry.register(AdminGUI2Endpoint.class);
		});
		try {
			mesh.run();
		} catch (Throwable t) {
			log.error("Error while starting mesh. Invoking shutdown.", t);
			mesh.shutdownAndTerminate(10);
		}
	}

}
