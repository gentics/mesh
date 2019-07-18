package com.gentics.mesh.server;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.verticle.admin.AdminGUIEndpoint;

import io.vertx.core.json.JsonObject;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class ServerRunner {

	static {
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
	}

	public static void main(String[] args) throws Exception {
		LoggingConfigurator.init();
		MeshOptions options = OptionsLoader.createOrloadOptions(args);

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
		options.getSearchOptions().setUrl(null);
		options.getSearchOptions().setStartEmbedded(false);

		Mesh mesh = Mesh.mesh(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());

			// Add admin ui
			EndpointRegistry registry = MeshInternal.get().endpointRegistry();
			registry.register(AdminGUIEndpoint.class);
		});
		mesh.run();
	}

}
