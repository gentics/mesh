package com.gentics.mesh.router;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

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

	/**
	 * Create a new plugin router.
	 * 
	 * @param chain
	 * @param db
	 * @param parentRouter
	 */
	public PluginRouter(MeshAuthChain chain, LegacyDatabase db, Router parentRouter) {
		this.router = Router.router(Mesh.vertx());

		// Ensure that all plugin requests are authenticated
		chain.secure(router.route());

		router.route().handler(rc -> {
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

	/**
	 * Return the plugin router with the given path segment name. A new router will be created if the router with the segment name can't be found.
	 * 
	 * @param name
	 * @return Existing or created router
	 */
	public Router getRouter(String name) {
		Router pluginRouter = pluginRouters.get(name);
		if (pluginRouter == null) {
			pluginRouter = Router.router(Mesh.vertx());
			log.info("Added plugin subrouter {" + name + "}");
			pluginRouters.put(name, pluginRouter);
		}
		router.mountSubRouter("/" + name, pluginRouter);
		return pluginRouter;
	}

}
