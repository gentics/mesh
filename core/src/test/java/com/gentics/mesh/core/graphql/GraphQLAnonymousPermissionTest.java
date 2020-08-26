package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import org.junit.Test;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.parameter.client.PublishParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLAnonymousPermissionTest extends AbstractMeshTest {

	@Test
	public void testReadViaAnonymous() throws Throwable {
		final String QUERY_NAME = "anonymous-perm-query";
		final HibNode node = folder("news");
		final String tagUuid = tx(() -> tag("red").getUuid());
		final String tagFamilyUuid = tx(() -> tag("red").getTagFamily().getUuid());
		final String nodeUuid = tx(() -> node.getUuid());
		final String anonRoleUuid = tx(() -> anonymousRole().getUuid());
		final String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(true);
		request.getPermissions().setRead(false).setReadPublished(true);
		call(() -> client().updateRolePermissions(anonRoleUuid, "projects/" + projectUuid(), request));
		request.getPermissions().setRead(true).setReadPublished(false);
		call(() -> client().updateRolePermissions(anonRoleUuid, "projects/" + projectUuid() + "/tagFamilies/" + tagFamilyUuid + "/tags/" + tagUuid,
			request));

		call(() -> client().addTagToNode(PROJECT_NAME, nodeUuid, tagUuid));
		call(() -> client().addTagToNode(PROJECT_NAME, baseNodeUuid, tagUuid));
		// Ensure that only two nodes are published
		call(() -> client().takeNodeOffline(PROJECT_NAME, baseNodeUuid, new PublishParametersImpl().setRecursive(true)));
		call(() -> client().publishNode(PROJECT_NAME, baseNodeUuid));
		call(() -> client().publishNode(PROJECT_NAME, nodeUuid));

		client().logout();

		// Now execute the query and assert it
		GraphQLResponse response = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(QUERY_NAME)));
		JsonObject jsonResponse = new JsonObject(response.toJson());
		System.out.println(jsonResponse.encodePrettily());
		assertThat(jsonResponse).compliesToAssertions(QUERY_NAME);
	}

}
