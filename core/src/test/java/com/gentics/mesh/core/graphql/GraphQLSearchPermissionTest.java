package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class GraphQLSearchPermissionTest extends AbstractMeshTest {

	@Test
	public void testReadPublishPerm() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}
		String queryName = "node-elasticsearch-perm-query";

		// 1. Create test node
		NodeResponse response = createNode("slug", FieldUtil.createStringField("blaar"));
		call(() -> client().publishNode(PROJECT_NAME, response.getUuid()));

		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.getFields().putString("name", "ABCD");
		nodeUpdateRequest.setLanguage("en");
		call(() -> client().updateNode(PROJECT_NAME, response.getUuid(), nodeUpdateRequest));

		// Verify that node can be initially found
		assertSearch(queryName, 1, 1);

		// 2. Revoke read permission and only grant read published
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		request.getPermissions().setReadPublished(true);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes/" + response.getUuid(), request));

		// 3. Assert node can be found
		assertSearch(queryName, 0, 1);

		// 4. Remove read publish perm
		request.getPermissions().setReadPublished(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes/" + response.getUuid(), request));

		// 5. Assert node can no longer be found
		assertSearch(queryName, 0, 0);

	}

	private void assertSearch(String queryName, int expectedDraftResults, int expectedPublishedResults) throws IOException {
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json).compliesToAssertions(queryName);

		JsonArray draftElements = (JsonArray) JsonPointer.from("/data/dresult/elements").queryJson(json);
		JsonArray publishedElements = (JsonArray) JsonPointer.from("/data/presult/elements").queryJson(json);

		assertThat(draftElements.size()).isEqualTo(expectedDraftResults);
		if (expectedDraftResults != 0) {
			String version = draftElements.getJsonObject(0).getString("version");
			assertEquals("1.1", version);
		}
		assertThat(publishedElements.size()).isEqualTo(expectedPublishedResults);
		if (expectedPublishedResults != 0) {
			String version = publishedElements.getJsonObject(0).getString("version");
			assertEquals("1.0", version);
		}
	}

}
