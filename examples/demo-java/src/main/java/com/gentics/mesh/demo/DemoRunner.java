package com.gentics.mesh.demo;

import static com.gentics.mesh.util.DeploymentUtil.deployAndWait;
import io.vertx.core.json.JsonObject;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.demo.verticle.CustomerVerticle;
import com.gentics.mesh.etc.ConfigurationLoader;
import com.gentics.mesh.etc.config.MeshConfiguration;
import com.gentics.mesh.verticle.admin.AdminGUIVerticle;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 * 
 * @author johannes2
 *
 */
public class DemoRunner {

	public static void main(String[] args) throws Exception {

		// For testing - We cleanup all the data. The customer module contains a class that will setup a fresh graph each startup.
		File graphDBDir = new File(System.getProperty("java.io.tmpdir"), "graphdb");
		FileUtils.deleteDirectory(graphDBDir);

		Mesh mesh = Mesh.mesh();

		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", 8080);
			deployAndWait(vertx, config, CustomerVerticle.class);
			// deployAndWait(vertx, AuthenticationVerticle.class);
			// deployAndWait(vertx, NavigationVerticle.class);
			// deployAndWait(vertx, TagCloudVerticle.class);
//			deployAndWait(vertx, config, StaticContentVerticle.class);
			deployAndWait(vertx, config, AdminGUIVerticle.class);
		});
		// // DeploymentOptions options = new DeploymentOptions();
		// // vertx.deployVerticle("service:com.gentics.vertx:mesh-rest-navigation:0.1.0-SNAPSHOT",options, dh -> {
		// // if (dh.failed()) {
		// // System.out.println(dh.cause());
		// // }
		// // });
		// // deployAndWait(vertx, "", "TestJSVerticle.js");
		// });

		// Setup custom config to enable neo4j web console
		MeshConfiguration config = ConfigurationLoader.createOrloadConfiguration();
		// config.getNeo4jConfiguration().setMode("gui");
		// config.getNeo4jConfiguration().setPath(graphDBDir.getAbsolutePath());
		mesh.run(config);

	}
}
