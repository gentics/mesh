package com.gentics.mesh.core.graphql;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointFilterTest extends AbstractMeshTest {

	@Test
	public void testIsContainerFilter() {
		String queryName = "filtering/isContainer";
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		JsonObject json = new JsonObject(response.toJson());
		JsonArray nodes = json.getJsonObject("data").getJsonObject("nodes").getJsonArray("elements");
		assertThat(nodes.size()).isGreaterThan(0);
		nodes.forEach(node -> {
			JsonObject nodeObj = (JsonObject) node;
			assertThat(nodeObj.getBoolean("isContainer")).isTrue();
		});
	}
}
