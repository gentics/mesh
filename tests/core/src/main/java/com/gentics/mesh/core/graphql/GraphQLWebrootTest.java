package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import org.junit.Test;

import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLWebrootTest extends AbstractMeshTest {

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

	@Test
	public void testSlashInPath() throws IOException {
		final String getUuidByPath = "webroot/get-uuid-by-path";
		final String getPathbyUuid = "webroot/get-path-by-uuid";
		final String uuid = "54a49cc79c684a2da407710a39427f6b";

		String parentUuid = client().findProjects().blockingGet().getData().get(0).getRootNode().getUuid();
		NodeCreateRequest request = new NodeCreateRequest();
		request.setParentNodeUuid(parentUuid);
		request.setSchemaName("folder");
		request.setLanguage("en");
		request.getFields().put("slug", new StringFieldImpl().setString("2015/2016"));
		client().createNode(uuid, PROJECT_NAME, request).blockingAwait();

		NodeResponse test = client()
			.findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl()
			.setResolveLinks(LinkType.SHORT)).blockingGet();

		GraphQLResponse response = singleParameterQuery(getPathbyUuid, "uuid", uuid);
		assertThat(new JsonObject(response.toJson())).compliesToAssertions(getPathbyUuid);
		String resultPath = response.getData().getJsonObject("node").getString("path");
		assertThat(resultPath).isEqualTo("/2015%2F2016");

		response = singleParameterQuery(getUuidByPath, "path", resultPath);
		assertThat(response.getData().getJsonObject("node").getString("uuid")).isEqualTo(uuid);
	}

	private GraphQLResponse singleParameterQuery(String queryName, String parameterName, String parameterValue) throws IOException {
		return client().graphql(PROJECT_NAME, new GraphQLRequest()
			.setQuery(getGraphQLQuery(queryName))
			.setVariables(new JsonObject().put(parameterName, parameterValue))
		).blockingGet();
	}
}
