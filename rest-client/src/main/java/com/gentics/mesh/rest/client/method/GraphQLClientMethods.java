package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

import io.vertx.core.json.JsonObject;

public interface GraphQLClientMethods {

	/**
	 * Execute the GraphQL command.
	 * 
	 * @param projectName
	 * @param query
	 * @param parameters
	 * @return
	 */
	MeshRequest<JsonObject> graphql(String projectName, String query, ParameterProvider... parameters);

	/**
	 * Execute the GraphQL query. Internally the given query will be wrapped within a JSON object and posted to the server.
	 * 
	 * @param projectName
	 * @param query
	 * @param parameters
	 * @return
	 */
	MeshRequest<JsonObject> graphqlQuery(String projectName, String query, ParameterProvider... parameters);
}
