package com.gentics.mesh.demo;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class RunnerNodeB {

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
	}

	public static void main(String[] args) throws Exception {
		MeshOptions options = OptionsLoader.createOrloadOptions();
		options.getStorageOptions().setDirectory("data-nodeB/graph");
		options.getSearchOptions().setDirectory("data-nodeB/es");
		options.getHttpServerOptions().setPort(8081);
		options.getHttpServerOptions().setEnableCors(true);
		options.getHttpServerOptions().setCorsAllowedOriginPattern("*");
		// options.getSearchOptions().setHttpEnabled(true);
		// options.getStorageOptions().setStartServer(false);
		options.getSearchOptions().setHttpEnabled(true);
		options.setClusterMode(true);
		// options.getStorageOptions().setDirectory(null);

		Mesh mesh = Mesh.mesh(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());
		});
		mesh.run();
	}

}
