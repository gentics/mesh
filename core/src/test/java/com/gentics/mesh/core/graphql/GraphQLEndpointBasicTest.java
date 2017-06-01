package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.json.JSONException;
import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.MeshJSONAssert;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointBasicTest extends AbstractMeshTest {

	@Test
	public void testIntrospection() {
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery("introspection-query")));
		assertNotNull(response);
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
	public void testDataFetchingError() throws Throwable {
		try (Tx tx = db().tx()) {
			role().revokePermissions(project(), READ_PERM);
		}
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, "{project{name}}"));
		System.out.println(response.getData().encodePrettily());
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
