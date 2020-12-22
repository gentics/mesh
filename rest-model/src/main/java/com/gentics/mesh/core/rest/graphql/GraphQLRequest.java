package com.gentics.mesh.core.rest.graphql;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

import io.vertx.core.json.JsonObject;

/**
 * POJO for a GraphQL request.
 */
public class GraphQLRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The actual GraphQL query.")
	private String query;

	@JsonProperty(required = false)
	@JsonPropertyDescription("GraphQL operation name.")
	private String operationName;

	@JsonProperty(required = false)
	@JsonPropertyDescription("JSON object which contains the variables.")
	private JsonObject variables;

	/**
	 * Return the GraphQL query.
	 * 
	 * @return
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Set the GraphQL query.
	 * 
	 * @param query
	 * @return Fluent API
	 */
	public GraphQLRequest setQuery(String query) {
		this.query = query;
		return this;
	}

	/**
	 * Return the operation name
	 */
	public String getOperationName() {
		return operationName;
	}

	/**
	 * Set the operation name.
	 * 
	 * @param operationName
	 * @return Fluent API
	 */
	public GraphQLRequest setOperationName(String operationName) {
		this.operationName = operationName;
		return this;
	}

	/**
	 * Return the query variables.
	 * 
	 * @return
	 */
	public JsonObject getVariables() {
		return variables;
	}

	/**
	 * Set the query variables.
	 * 
	 * @param variables
	 * @return Fluent API
	 */
	public GraphQLRequest setVariables(JsonObject variables) {
		this.variables = variables;
		return this;
	}
}
