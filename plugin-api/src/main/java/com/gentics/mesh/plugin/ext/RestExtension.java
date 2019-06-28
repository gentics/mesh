package com.gentics.mesh.plugin.ext;

import org.pf4j.ExtensionPoint;

import io.vertx.ext.web.Router;

public interface RestExtension extends ExtensionPoint {

	/**
	 * Method which will register the endpoints of the plugin. Note that this method will be invoked multiple times in order to register the endpoints to all
	 * REST verticles.
	 * 
	 * @param globalRouter
	 * @param projectRouter
	 */
	void registerEndpoints(Router globalRouter, Router projectRouter);

}
