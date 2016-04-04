package com.gentics.mesh.server;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.DeploymentUtil;
import com.gentics.mesh.verticle.admin.AdminGUIVerticle;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class ServerRunner {

	static {
		//		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		//		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
	}

	public static void main(String[] args) throws Exception {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		MeshOptions options = OptionsLoader.createOrloadOptions();
		Mesh mesh = Mesh.mesh(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());
			DeploymentUtil.deployAndWait(vertx, config, AdminGUIVerticle.class, false);
		});
		mesh.run();
	}
}
