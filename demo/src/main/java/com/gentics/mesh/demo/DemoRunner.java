package com.gentics.mesh.demo;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.demo.verticle.DemoVerticle;
import com.gentics.mesh.etc.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.DeploymentUtil;
import com.gentics.mesh.verticle.admin.AdminGUIVerticle;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class DemoRunner {

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
	}

	public static void main(String[] args) throws Exception {
		MeshOptions options = OptionsLoader.createOrloadOptions();
		options.getHttpServerOptions().setEnableCors(true);
		options.getHttpServerOptions().setCorsAllowedOriginPattern("*");
		options.getStorageOptions().setDirectory(null);
		Mesh mesh = Mesh.mesh(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());
			DeploymentUtil.deployAndWait(vertx, config, DemoVerticle.class, false);
			DeploymentUtil.deployAndWait(vertx, config, AdminGUIVerticle.class, false);
		});
		mesh.run();
	}
}
