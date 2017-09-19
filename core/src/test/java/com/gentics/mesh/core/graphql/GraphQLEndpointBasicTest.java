package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.json.JSONException;
import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.MeshJSONAssert;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointBasicTest extends AbstractMeshTest {

	@Test
	public void testIntrospection() throws IOException {
		try (Tx tx = tx()) {
			for (MicroschemaContainer microschema : meshRoot().getMicroschemaContainerRoot().findAll()) {
				microschema.remove();
			}
			tx.success();
		}
		String queryName = "introspection-query";
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		JsonObject json = new JsonObject(JsonUtil.toJson(response));
		assertThat(json).compliesToAssertions(queryName);
	}

	@Test
	public void testSimpleQuery() throws JSONException {
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, "{me{firstname}}"));
		MeshJSONAssert.assertEquals("{'me':{'firstname':'Joe'}}", response.getData());
	}

	@Test
	public void testEmptyQuery() throws Throwable {
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, ""));
		assertEquals(1, response.getErrors().stream().filter(error -> error.getType().equals("InvalidSyntax")).count());
	}

	@Test
	public void testErrorHandling() throws Throwable {
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, "{bogus{firstname}}"));
		assertEquals(1, response.getErrors().stream().filter(error -> error.getType().equals("ValidationError")).count());
	}

	@Test
	public void testVariables() throws Throwable {
		GraphQLRequest request = new GraphQLRequest();
		request.setQuery("query test($var: String) { node(path: $var) { node { uuid } } }");
		request.setVariables(new JsonObject().put("var", "/News"));
		GraphQLResponse response = call(() -> client().graphql(PROJECT_NAME, request));
		String uuid = response.getData().getJsonObject("node").getJsonObject("node").getString("uuid");
		assertThat(uuid).isNotEmpty();
	}
}
