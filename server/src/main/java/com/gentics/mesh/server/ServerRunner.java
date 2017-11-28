package com.gentics.mesh.server;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.MissingOrientCredentialFixer;
import com.gentics.mesh.search.verticle.ElasticsearchHeadVerticle;
import com.gentics.mesh.util.DeploymentUtil;
import com.gentics.mesh.verticle.admin.AdminGUIVerticle;

import io.vertx.core.json.JsonObject;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class ServerRunner {

	static {
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
		System.setProperty("storage.trackChangedRecordsInWAL", "true");
	}

	public static void main(String[] args) throws Exception {
		LoggingConfigurator.init();
		MeshOptions options = OptionsLoader.createOrloadOptions(args);
		MissingOrientCredentialFixer.fix(options);

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
