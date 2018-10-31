package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.FULL, startServer = true)
public class GraphQLWebrootPathPrefixTest extends AbstractMeshTest {

	@Test
	public void testWebrootPrefix() throws IOException {
		final String queryName = "webroot/node-webroot-path-prefix-query";
		final String prefix = "some/prefix";

		BranchUpdateRequest request = new BranchUpdateRequest();
		request.setPathPrefix(prefix);
		call(() -> client().updateBranch(PROJECT_NAME, initialBranchUuid(), request));

		// Now execute the query and assert it
		GraphQLResponse response = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		assertThat(new JsonObject(response.toJson())).compliesToAssertions(queryName);
	}
}
