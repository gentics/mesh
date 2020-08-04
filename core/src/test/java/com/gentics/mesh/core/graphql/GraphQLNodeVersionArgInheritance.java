package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

/**
 * This test will verify that the inheritance mechanism for the node type argument works as expected.
 */
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLNodeVersionArgInheritance extends AbstractGraphQLNodeTest {

	@Before
	public void setupContent() {
		setupContents(true);
	}

	@Test
	public void testPermissions() throws IOException {
		String queryName ="node/version-arg";
		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(getGraphQLQuery(queryName));
		GraphQLResponse response = call(() -> client().graphql(PROJECT_NAME, request));
		JsonObject jsonResponse = new JsonObject(response.toJson());
		System.out.println(jsonResponse.encodePrettily());
		assertThat(jsonResponse).compliesToAssertions(queryName);
	}
}
