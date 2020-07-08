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
		MeshOptions defaultOption = new MeshOptions();
		defaultOption.getSearchOptions().setStartEmbedded(false);
		defaultOption.getSearchOptions().setUrl(null);
		MeshOptions options = OptionsLoader.createOrloadOptions(defaultOption, args);

		Mesh mesh = Mesh.create(options);
		mesh.setCustomLoader((vertx) -> {
			mesh.deployPlugin(AuthPlugin.class, "auth").subscribe(() -> System.out.println("plugin deployed!"));
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
