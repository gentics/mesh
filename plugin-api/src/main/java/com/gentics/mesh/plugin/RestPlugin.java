package com.gentics.mesh.plugin;

import io.vertx.ext.web.Router;

/**
 * A REST Plugin is an plugin which will extend the REST API of Gentics Mesh.
 */
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
	 * Return the API name for the REST plugin. By default the plugin id will be used for the API name.
	 */
	default String apiName() {
		return id();
	}

}
