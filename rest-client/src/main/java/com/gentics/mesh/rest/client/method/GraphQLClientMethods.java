package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

import io.vertx.core.json.JsonObject;

public interface GraphQLClientMethods {

	/**
	 * Execute the GraphQL query.
	 * 
	 * @param projectName
	 * @param query
	 * @param parameters
	 * @return
	 */
	MeshRequest<JsonObject> graphql(String projectName, String query, ParameterProvider... parameters);
}
