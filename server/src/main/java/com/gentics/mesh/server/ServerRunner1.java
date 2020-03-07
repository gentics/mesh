package com.gentics.mesh.server;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.verticle.admin.AdminGUIEndpoint;

import io.vertx.core.json.JsonObject;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class ServerRunner1 {

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
		options.setInitCluster(true);
		options.getAuthenticationOptions().setKeystorePassword("finger");
		options.setInitialAdminPassword("admin");
		options.setForceInitialAdminPasswordReset(false);

		options.getClusterOptions().setCoordinatorMode(CoordinatorMode.FULL);
		options.getStorageOptions().setStartServer(true);
		options.getClusterOptions().setClusterName("test");
		options.getClusterOptions().setEnabled(true);
		options.setNodeName("node1");
		options.getStorageOptions().setDirectory("data1/graphdb");
		options.getClusterOptions().setVertxPort(6151);
		options.getHttpServerOptions().setPort(8081);
		options.getMonitoringOptions().setPort(8881);
		options.getSearchOptions().setUrl(null);
		options.getSearchOptions().setStartEmbedded(false);
		options.getDebugInfoOptions().setLogEnabled(false);

		Mesh mesh = Mesh.create(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());

			// Add admin ui
			MeshComponent meshInternal = mesh.internal();
			EndpointRegistry registry = meshInternal.endpointRegistry();
			registry.register(AdminGUIEndpoint.class);

		});
		mesh.run();
	}

}
