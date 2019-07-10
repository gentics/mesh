package com.gentics.mesh.plugin;

import io.vertx.ext.web.Router;

public interface RestPlugin extends MeshPlugin {

	/**
	 * Method which will register the endpoints of the plugin. Note that this method will be invoked multiple times in order to register the endpoints to all
	 * REST verticles.
	 * 
	 * @param globalRouter
	 * @param projectRouter
	 */
	void registerEndpoints(Router globalRouter, Router projectRouter);

	/**
	 * Return the API name for the REST plugin.
	 */
	String apiName();

}
