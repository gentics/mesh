package com.gentics.mesh.server;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.verticle.ElasticsearchHeadVerticle;
import com.gentics.mesh.util.DeploymentUtil;
import com.gentics.mesh.verticle.admin.AdminGUIVerticle;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class ServerRunner {

	private static final Logger log;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		System.setProperty("storage.trackChangedRecordsInWAL", "true");
		log = LoggerFactory.getLogger(ServerRunner.class);
	}

	public static void main(String[] args) throws Exception {
		MeshOptions options = OptionsLoader.createOrloadOptions(args);
		Mesh mesh = Mesh.mesh(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());

			// Add admin ui
			AdminGUIVerticle adminVerticle = new AdminGUIVerticle(MeshInternal.get().routerStorage());
			DeploymentUtil.deployAndWait(vertx, config, adminVerticle, false);

			// Add elastichead
			if (options.getSearchOptions().isHttpEnabled()) {
				ElasticsearchHeadVerticle headVerticle = new ElasticsearchHeadVerticle(MeshInternal.get().routerStorage());
				DeploymentUtil.deployAndWait(vertx, config, headVerticle, false);
			}
		});
		mesh.run();
	}

}
