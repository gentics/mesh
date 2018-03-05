package com.gentics.mesh.router;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.Mesh;

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

	public PluginRouter(Router parentRouter) {
		this.router = Router.router(Mesh.vertx());
		parentRouter.mountSubRouter(PLUGINS_MOUNTPOINT, router);
	}

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
