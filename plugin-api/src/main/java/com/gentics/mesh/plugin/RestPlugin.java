package com.gentics.mesh.plugin;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * A REST Plugin is an plugin which will extend the REST API of Gentics Mesh.
 */
public interface RestPlugin extends MeshPlugin {

	/**
	 * Note that this method will be invoked multiple times in order to register the endpoints to all REST verticles.
	 */
	default Router createGlobalRouter() {
		return null;
	}

	/**
	 * Note that this method will be invoked multiple times in order to register the endpoints to all REST verticles.
	 * 
	 * @return
	 */
	default Router createProjectRouter() {
		return null;
	}

	/**
	 * Return a wrapped routing context.
	 * 
	 * @param rc
	 *            Vert.x routing context
	 * @return Wrapped context
	 */
	default PluginContext wrap(RoutingContext rc) {
		return new PluginContext(rc, environment());
	}

	/**
	 * Return a wrapped routing context handler
	 * 
	 * @param handler
	 *            Handler to be wrapped
	 * @return Wrapped handler
	 */
	default Handler<RoutingContext> wrapHandler(Handler<PluginContext> handler) {
		return rc -> handler.handle(wrap(rc));
	}

	/**
	 * Return the API name for the REST plugin. By default the plugin id will be used for the API name.
	 * 
	 * @return name for the api
	 */
	default String restApiName() {
		return id();
	}

}
