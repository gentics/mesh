package com.gentics.mesh.router;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * Central router for plugin REST extensions.
 */
public class PluginRouter {

	public static final String PLUGINS_MOUNTPOINT = "/plugins";

	private static final Logger log = LoggerFactory.getLogger(APIRouter.class);

	private Map<String, Router> pluginRouters = new HashMap<>();

	private Router router;

	private final Vertx vertx;

	/**
	 * Create a new plugin router.
	 * 
	 * @param vertx
	 * @param chain
	 * @param db
	 * @param parentRouter
	 */
	public PluginRouter(Vertx vertx, MeshAuthChain chain, Database db, Router parentRouter) {
		this.vertx = vertx;
		this.router = Router.router(vertx);

		// Ensure that all plugin requests are authenticated
		chain.secure(router.route());

		router.route().blockingHandler(rc -> {
			Project project = (Project) rc.data().get(ProjectsRouter.PROJECT_CONTEXT_KEY);
			if (project != null) {
				db.tx(() -> {
					JsonObject projectInfo = new JsonObject();
					projectInfo.put("uuid", project.getUuid());
					projectInfo.put("name", project.getName());
					rc.data().put("mesh.project", projectInfo);
				});
			}
			rc.next();
		});

		parentRouter.mountSubRouter(PLUGINS_MOUNTPOINT, router);
	}

	public void addRouter(String name, Router pluginRouter) {
		pluginRouters.put(name, pluginRouter);
		router.mountSubRouter("/" + name, pluginRouter);
		log.info("Added plugin subrouter {" + name + "}");
	}

	public void removeRouter(String name) {
		Router pluginRouter = pluginRouters.get(name);
		if (pluginRouter != null) {
			pluginRouter.clear();
			pluginRouters.remove(name);
			log.info("Removed plugin subrouter {" + name + "}");
		}
	}

}
