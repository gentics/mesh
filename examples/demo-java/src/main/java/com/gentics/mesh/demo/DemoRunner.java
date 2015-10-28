package com.gentics.mesh.demo;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.demo.verticle.CustomerVerticle;
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
	}

	public static void main(String[] args) throws Exception {

		// For testing - We cleanup all the data. The customer module contains a class that will setup a fresh graph each startup.
		//File graphDBDir = new File(System.getProperty("java.io.tmpdir"), "graphdb");
		//FileUtils.deleteDirectory(graphDBDir);
		MeshOptions options = OptionsLoader.createOrloadOptions();
		options.getStorageOptions().setDirectory(null);
		Mesh mesh = Mesh.mesh(options);

		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", 8080);
			DeploymentUtil.deployAndWait(vertx, config, CustomerVerticle.class);
			// deployAndWait(vertx, AuthenticationVerticle.class);
			// deployAndWait(vertx, NavigationVerticle.class);
			// deployAndWait(vertx, TagCloudVerticle.class);
			// deployAndWait(vertx, config, StaticContentVerticle.class);
			DeploymentUtil.deployAndWait(vertx, config, AdminGUIVerticle.class);
		});
		mesh.run();

	}
}
