package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.MeshJSONAssert;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointBasicTest extends AbstractMeshTest {

	@Test
	public void testIntrospection() {
		JsonObject response = call(() -> client().graphql(PROJECT_NAME, getGraphQLQuery("introspection-query")));
		assertNotNull(response);
	}

	@Test
	public void testSimpleQuery() throws JSONException {
		JsonObject response = call(() -> client().graphqlQuery(PROJECT_NAME, "{me{firstname}}"));
		MeshJSONAssert.assertEquals("{'data':{'me':{'firstname':'Joe'}}}", response);
	}

	@Test
	public void testEmptyQuery() throws Throwable {
		JsonObject response = call(() -> client().graphqlQuery(PROJECT_NAME, ""), BAD_REQUEST);
		assertFalse("The errors array should not be empty.", response.getJsonArray("errors")
				.isEmpty());
	}

	@Test
	public void testDataFetchingError() throws Throwable {
		try (NoTx noTx = db().noTx()) {
			role().revokePermissions(project(), READ_PERM);
		}
		JsonObject response = call(() -> client().graphqlQuery(PROJECT_NAME, "{project{name}}"), BAD_REQUEST);
		System.out.println(response.encodePrettily());
	}

	@Test
	public void testErrorHandling() throws Throwable {
		JsonObject response = call(() -> client().graphqlQuery(PROJECT_NAME, "{bogus{firstname}}"), BAD_REQUEST);
		assertFalse("The errors array should not be empty.", response.getJsonArray("errors")
				.isEmpty());
	}

	@Test
	public void testVariables() throws Throwable {
		JsonObject query = new JsonObject()
				.put("query", "query test($var: String) { node(path: $var) { node { uuid } } }")
				.put("variables", new JsonObject()
						.put("var", "/News")
				);
		JsonObject response = call(() -> client().graphql(PROJECT_NAME, query.toString()));
		String uuid = response.getJsonObject("data").getJsonObject("node").getJsonObject("node").getString("uuid");

		assertThat(uuid).isNotEmpty();
	}
}
