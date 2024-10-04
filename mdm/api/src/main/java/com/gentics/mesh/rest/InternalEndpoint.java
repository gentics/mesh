package com.gentics.mesh.rest;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.router.RouterStorage;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * An endpoint represents a specific path in the REST API which exposes various endpoint routes.
 */
public interface InternalEndpoint {

	/**
	 * Wrap the routing context.
	 * 
	 * @param rc
	 * @return
	 */
	InternalActionContext wrap(RoutingContext rc) ;

	/**
	 * Create a new endpoint. Internally a new route will be wrapped.
	 * 
	 * @return Created endpoint
	 */
	InternalEndpointRoute createRoute();

	/**
	 * Register all endpoints to the local router.
	 */
	void registerEndPoints();

	/**
	 * Initialize this endpoint.
	 * 
	 * @param vertx
	 * @param rs
	 */
	void init(Vertx vertx, RouterStorage rs);
}
