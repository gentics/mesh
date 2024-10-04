package com.gentics.mesh.router;

import java.util.Map;

import io.vertx.ext.web.Router;

/**
 * This class manages the project routers (e.g. routers for endpoints like :apibase:/:projectName/nodes)
 */
public interface ProjectRouter {

	/**
	 * Return the Vert.x router.
	 * 
	 * @return
	 */
	Router getRouter();

	/**
	 * Return all routers that have been registered.
	 * 
	 * @return
	 */
	Map<String, Router> getRouters();

	/**
	 * Return the project plugin router.
	 * 
	 * @return
	 */
	PluginRouter pluginRouter();

	/**
	 * Return the registered project subrouter.
	 * 
	 * @return the router or null if no router was found
	 */
	Router getOrCreate(String name);

}
