package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true)
public class GraphQLMeshTypeTest extends AbstractMeshTest {

	@Test
	public void testMeshType() throws IOException {
		options().getHttpServerOptions().setServerTokens(true);
		assertType("mesh/mesh-query");
	}

	@Test
	public void testWithDisabledServerTokens() throws IOException {
		options().getHttpServerOptions().setServerTokens(false);
		assertType("mesh/mesh-no-servertoken-query");
	}

	public void assertType(String queryName) throws IOException {
		GraphQLResponse response = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json).compliesToAssertions(queryName);
	}

}
