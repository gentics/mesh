package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;

import java.io.IOException;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

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

		// Verify that node can be initially found
		assertSearch(queryName, 1, false);

		// 2. Revoke read permission and only grant read published
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		request.getPermissions().setReadPublished(true);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes/" + response.getUuid(), request));

		// 3. Assert node can be found
		assertSearch(queryName, 1, true);

		// 4. Remove read publish perm
		request.getPermissions().setReadPublished(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes/" + response.getUuid(), request));

		// 5. Assert node can no longer be found
		assertSearch(queryName, 0, true);

	}

	private void assertSearch(String queryName, int expectedResults, boolean readPublished) throws IOException {
		VersioningParametersImpl params = new VersioningParametersImpl();
		if (readPublished) {
			params.published();
		}
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName), params));
		JsonObject json = new JsonObject(response.toJson());
		System.out.println(json.encodePrettily());
		assertThat(json).compliesToAssertions(queryName);
	}

}
