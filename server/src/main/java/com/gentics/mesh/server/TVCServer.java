package com.gentics.mesh.server;

import static com.gentics.mesh.util.DeploymentUtil.deployAndWait;
import io.vertx.core.json.JsonObject;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.cli.MeshImpl;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.verticle.admin.AdminGUIVerticle;

public class TVCServer extends MeshImpl {

	public TVCServer(MeshOptions options) {
		super(options);
	}

	public static void main(String[] args) throws Exception {
		// TODO errors should be handled by a logger
		Mesh mesh = Mesh.initalize();
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", 8080);
			// deployAndWait(vertx, AuthenticationVerticle.class);
			// deployAndWait(vertx, NavigationVerticle.class);
			// deployAndWait(vertx, TagCloudVerticle.class);
			//			deployAndWait(vertx, config, StaticContentVerticle.class);
			deployAndWait(vertx, config, AdminGUIVerticle.class);
		});
		mesh.handleArguments(args);
		mesh.run();
	}

}
