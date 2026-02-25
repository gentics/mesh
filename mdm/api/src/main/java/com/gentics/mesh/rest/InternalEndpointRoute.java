package com.gentics.mesh.rest;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.gentics.mesh.core.rest.MeshEvent;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Simple wrapper for vert.x routes. The wrapper is commonly used to generate RAML descriptions for the route.
 */
public interface InternalEndpointRoute extends com.gentics.vertx.openapi.metadata.InternalEndpointRoute {

	/**
	 * Set the endpoint json example request via the provided json object. The JSON schema will not be generated.
	 * 
	 * @param jsonObject
	 * @return Fluent API
	 */
	InternalEndpointRoute exampleRequest(JSONObject jsonObject);

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

	@Override
	default com.gentics.vertx.openapi.metadata.InternalEndpointRoute exampleRequest(JsonObject jsonObject) {
		try {
			return exampleRequest(new JSONObject(jsonObject.encode()));
		} catch (JSONException e) {
			throw error(INTERNAL_SERVER_ERROR, "error_internal", e);
		}
	}
}
