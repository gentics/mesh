package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

/**
 * REST clients methods for GraphQL operations.
 */
public interface GraphQLClientMethods {

	/**
	 * Execute the GraphQL command.
	 * 
	 * @param projectName
	 * @param request
	 * @param parameters
	 * @return
	 */
	MeshRequest<GraphQLResponse> graphql(String projectName, GraphQLRequest request, ParameterProvider... parameters);

	/**
	 * Execute the GraphQL query. Internally the given query will be wrapped within a JSON object and posted to the server.
	 * 
	 * @param projectName
	 * @param query
	 * @param parameters
	 * @return
	 */
	default MeshRequest<GraphQLResponse> graphqlQuery(String projectName, String query, ParameterProvider... parameters) {
		return graphql(projectName, new GraphQLRequest().setQuery(query), parameters);
	}
}
