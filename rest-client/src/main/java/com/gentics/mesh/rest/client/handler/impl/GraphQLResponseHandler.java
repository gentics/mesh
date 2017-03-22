package com.gentics.mesh.rest.client.handler.impl;

import static io.vertx.core.http.HttpMethod.POST;

import com.gentics.mesh.rest.client.handler.AbstractResponseHandler;
import com.gentics.mesh.rest.client.handler.JsonObjectErrorHandler;
import com.gentics.mesh.rest.client.handler.JsonObjectSuccessHandler;

import io.vertx.core.json.JsonObject;

/**
 * GraphQL response handler which returns plain {@link JsonObject}'s for errors and regular responses.
 */
public class GraphQLResponseHandler extends AbstractResponseHandler<JsonObject>
		implements JsonObjectErrorHandler<JsonObject>, JsonObjectSuccessHandler {

	public GraphQLResponseHandler(String uri) {
		super(POST, uri);
	}

}
