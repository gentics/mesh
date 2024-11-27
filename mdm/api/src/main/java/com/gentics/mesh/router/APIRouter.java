package com.gentics.mesh.router;

import io.vertx.ext.web.Router;

/**
 * Central router for /api/v1 routes
 */
public interface APIRouter {

	/**
	 * Internal vert.x router for the API router.
	 * 
	 * @return
	 */
	Router getRouter();

	/**
	 * Return the router to which all projects will be mounted.
	 * 
	 * @return
	 */
	ProjectsRouter projectsRouter();

	/**
	 * Returns the plugin router which can be used to create routers for plugins.
	 * 
	 * @return
	 */
	PluginRouter pluginRouter();

	/**
	 * Get a core api subrouter. A new router will be created id no existing one could be found.
	 * 
	 * @param mountPoint
	 * @return existing or new router
	 */
	Router createSubRouter(String mountPoint);

}
