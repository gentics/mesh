package com.gentics.mesh.example;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;

import io.vertx.core.json.JsonObject;

public class GraphQLExamples extends AbstractExamples {

	public GraphQLRequest createQueryRequest() {
		return new GraphQLRequest().setQuery("{ me { username } }");
	}

	public GraphQLResponse createResponse() {
		JsonObject json = new JsonObject();
		json.put("me", new JsonObject().put("username", "anonymous"));
		return new GraphQLResponse().setData(json);
	}

}
