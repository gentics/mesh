package com.gentics.mesh.server.cluster;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.verticle.admin.AdminGUI2Endpoint;
import com.gentics.mesh.verticle.admin.AdminGUIEndpoint;

import io.vertx.core.json.JsonObject;

public class ClusterServer {

	public static MeshOptions init(String[] args) {
		LoggingConfigurator.init();
		MeshOptions options = OptionsLoader.createOrloadOptions(args);

		// options.setAdminPassword("admin");
		// options.getStorageOptions().setStartServer(true);
		// options.getHttpServerOptions().setCorsAllowCredentials(true);
		// options.getHttpServerOptions().setEnableCors(true);
		// options.getHttpServerOptions().setCorsAllowedOriginPattern("http://localhost:5000");
		options.getAuthenticationOptions().setKeystorePassword("finger");
		options.setInitialAdminPassword("admin");
		options.setForceInitialAdminPasswordReset(false);

		//options.getClusterOptions().setCoordinatorMode(CoordinatorMode.ALL);
		//options.getClusterOptions().setCoordinatorRegex("gentics-mesh-[0-9]");
		options.getStorageOptions().setStartServer(true);
		options.getClusterOptions().setClusterName("test");
		options.getClusterOptions().setEnabled(true);
		options.getSearchOptions().setUrl(null);
		options.getSearchOptions().setStartEmbedded(false);
		options.getDebugInfoOptions().setLogEnabled(false);

		// New settings
		options.getStorageOptions().setSynchronizeWrites(true);
		options.getStorageOptions().setSynchronizeWritesTimeout(90_000);
		options.getClusterOptions().setTopologyLockDelay(20_000);
		options.getClusterOptions().setTopologyLockTimeout(240_000);
		options.getStorageOptions().setTxCommitTimeout(10_000);
		return options;
	}

	public static void run(MeshOptions options) throws Exception {
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
		mesh.run();

	}
}
