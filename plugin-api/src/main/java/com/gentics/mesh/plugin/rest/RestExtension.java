package com.gentics.mesh.plugin.rest;

import io.vertx.ext.web.Router;

public interface RestExtension {

	/**
	 * Return the router that can be used to add custom routes for this extension.
	 * 
	 * @return
	 */
	Router router();

	/**
	 * Name of the extension. This string will also be used to locate the extension in the REST API.
	 * 
	 * @return
	 */
	String name();

	/**
	 * Initialize the extension using the provided router. This method will be invoked once the extension is being processed by the Gentics Mesh server.
	 * 
	 * @param router
	 */
	void init(Router router);

	/**
	 * Scope of the extension. The extension can for example be global or scoped to projects.
	 * 
	 * @return
	 */
	RestExtensionScope scope();

	/**
	 * Method which can be implemented in order to setup routes.
	 */
	void start();

}
