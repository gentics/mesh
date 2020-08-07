package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLPermissionTest extends AbstractMeshTest {

	@Test
	public void testReadPublishedNodeChildren() throws IOException {

		// 1. Publish all nodes
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());
		call(() -> client().publishNode(PROJECT_NAME, baseNodeUuid, new PublishParametersImpl().setRecursive(true)));

		// 2. Take deals node offline
		String dealsUuid = tx(() -> folder("deals").getUuid());
		call(() -> client().takeNodeOffline(PROJECT_NAME, dealsUuid, new PublishParametersImpl().setRecursive(true)));

		// 3. Revoke all read perm from all nodes also read_published from /News
		try (Tx tx = tx()) {
			RoleRoot roleDao = tx.data().roleDao();
			for (Node node : project().getNodeRoot().findAll()) {
				roleDao.revokePermissions(role(), node, GraphPermission.READ_PERM);
			}
			// Explicitly remove read_publish for a single node
			roleDao.revokePermissions(role(), folder("news"), GraphPermission.READ_PUBLISHED_PERM);
			tx.success();
		}

		// This assertion fails. The children list should not contain the Deals node since the user has no perms on it.
		// Assert that it is not possible to load nodes draft version
		assertQuery();

		// Assert that using version param does not affect the handling
		assertQuery(new VersioningParametersImpl().setVersion("published"));

	}

	private void assertQuery(ParameterProvider... parameters) throws IOException {
		String queryName2 = "node-perm-children-query";
		GraphQLResponse response2 = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName2), parameters));
		JsonObject json2 = new JsonObject(response2.toJson());
		System.out.println(json2.encodePrettily());
		assertThat(json2).compliesToAssertions(queryName2);
	}

}
