package com.gentics.mesh.router;

import io.vertx.ext.web.Router;

/**
 * Router to track plugin sub routers.
 */
public interface PluginRouter {

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

}
