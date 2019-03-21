package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.FULL, startServer = true)
public class GraphQLAnonymousPermissionTest extends AbstractMeshTest {

	@Test
	public void testReadViaAnonymous() throws Throwable {
		final String QUERY_NAME = "anonymous-perm-query";
		Node node = folder("2015");
		String nodeUuid = tx(() -> node.getUuid());
		String anonRoleUuid = tx(() -> anonymousRole().getUuid());

		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(true);
		request.getPermissions().setRead(false).setReadPublished(true);
		call(() -> client().updateRolePermissions(anonRoleUuid, "projects/" + projectUuid(), request));

		// Ensure that the node is published
		call(() -> client().publishNode(PROJECT_NAME, nodeUuid));

		client().logout();

		// Now execute the query and assert it
		GraphQLResponse response = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(QUERY_NAME)));
		JsonObject jsonResponse = new JsonObject(response.toJson());
		assertThat(jsonResponse).compliesToAssertions(QUERY_NAME);
	}

}
