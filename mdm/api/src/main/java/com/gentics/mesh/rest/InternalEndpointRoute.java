package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.MeshEvent;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Simple wrapper for vert.x routes. The wrapper is commonly used to generate RAML descriptions for the route.
 */
public interface InternalEndpointRoute extends com.gentics.vertx.openapi.metadata.InternalEndpointRoute {

	/**
	 * Set the events which are emitted by the action of the endpoint.
	 * 
	 * @param events
	 * @return Fluent API
	 */
	InternalEndpointRoute events(MeshEvent... events);

	/**
	 * @see com.gentics.vertx.openapi.metadata.InternalEndpointRoute#blockingHandler(Handler)
	 *
	 * @deprecated since requests will only be "ordered" when running in the same http verticle
	 */
	@Override
	@Deprecated
	InternalEndpointRoute blockingHandler(Handler<RoutingContext> requestHandler);

	/**
	 * If true, the endpoint can be used with no authentication. 
	 * 
	 * @return
	 */
	boolean isInsecure();

	/**
	 * Set the endpoint to omit the secure token requirement.
	 * 
	 * @param insecure
	 * @return
	 */
	InternalEndpointRoute setInsecure(boolean insecure);
}
