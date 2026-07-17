package com.gentics.mesh.router;

import java.util.Map;

import io.vertx.ext.web.Router;

/**
 * Router to track plugin sub routers.
 */
public interface PluginRouter extends InternalRouter {

	/**
	 * Remove the plugin router.
	 * 
	 * @param name
	 */
	void removeRouter(String name);

	/**
	 * Register a plugin sub router.
	 * 
	 * @param name
	 * @param pluginRouter
	 */
	void addRouter(String name, Router pluginRouter);

	/**
	 * Get currently registered routers.
	 * 
	 * @return immutable name/router map
	 */
	Map<String, Router> pluginRouters();
}
