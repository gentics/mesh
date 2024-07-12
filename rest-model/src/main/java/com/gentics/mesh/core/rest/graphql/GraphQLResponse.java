package com.gentics.mesh.core.rest.graphql;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

import io.vertx.core.json.JsonObject;

/**
 * POJO for a graphql response. The actual JSON data is nested within.
 */
public class GraphQLResponse implements RestModel {

	/**
	 * Object which contains the response data.
	 */
	@JsonProperty(required = true)
	@JsonPropertyDescription("Nested JSON object which contains the query result data.")
	private JsonObject data;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Array of errors which were encoutered when handling the query.")
	private List<GraphQLError> errors;

	/**
	 * Return the response data.
	 * 
	 * @return
	 */
	public JsonObject getData() {
		return data;
	}

	/**
	 * Set the GraphQL query response data.
	 * 
	 * @param data
	 * @return
	 */
	public GraphQLResponse setData(JsonObject data) {
		this.data = data;
		return this;
	}

	/**
	 * Return a list of errors.
	 * 
	 * @return
	 */
	public List<GraphQLError> getErrors() {
		return errors;
	}

	/**
	 * Set errors which were encountered while evaluating the query.
	 * 
	 * @param errors
	 */
	public void setErrors(List<GraphQLError> errors) {
		this.errors = errors;
	}
}
