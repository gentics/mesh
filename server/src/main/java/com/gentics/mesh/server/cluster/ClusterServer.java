package com.gentics.mesh.server.cluster;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A server that can be run in a cluster.
 */
public abstract class ClusterServer {

	public static Logger log;

	/**
	 * initialize clustering options
	 * @param args
	 * @return
	 */
	public static HibernateMeshOptions init(String[] args) {
		LoggingConfigurator.init();
		log = LoggerFactory.getLogger(ClusterServer.class);

		HibernateMeshOptions defaultOption = new HibernateMeshOptions();
		defaultOption.getSearchOptions().setUrl(null);
		HibernateMeshOptions options = OptionsLoader.createOrloadOptions(HibernateMeshOptions.class, defaultOption, args);
		options.getClusterOptions().setClusterName("test");
		options.getClusterOptions().setEnabled(true);

		options.setInitialAdminPassword("admin");
		options.setForceInitialAdminPasswordReset(false);

		return options;
	}

	/**
	 * Run the server with the given options.
	 *
	 * @param options
	 * @throws Exception
	 */
	public static void run(MeshOptions options) {
		Mesh mesh = Mesh.create(options);
		mesh.setCustomLoader(vertx -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());
		});

		try {
			mesh.run();
		} catch (Throwable t) {
			log.error("Error while starting mesh. Invoking shutdown.", t);
			mesh.shutdownAndTerminate(10);
		}
	}
}
