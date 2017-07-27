package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.ClientHelper.call;

import java.io.IOException;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.FULL, startServer = true)
public class GraphQLPermissionTest extends AbstractMeshTest {

	@Test
	public void testReadPublishedNodeChildren() throws IOException {

		// 1. Publish all nodes
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());
		call(() -> client().publishNode(PROJECT_NAME, baseNodeUuid, new PublishParametersImpl().setRecursive(true)));

		// 2. Revoke all read perm
		try (Tx tx = tx()) {
			for (Node node : project().getNodeRoot().findAll()) {
				role().revokePermissions(node, GraphPermission.READ_PERM);
			}
			role().revokePermissions(folder("news"), GraphPermission.READ_PUBLISHED_PERM);
			tx.success();
		}

		// 3. Invoke the query and assert that the nodes can still be loaded
		String queryName = "node-perm-children-query";
		GraphQLResponse response = call(
				() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName), new VersioningParametersImpl().setVersion("published")));
		JsonObject json = new JsonObject(JsonUtil.toJson(response));
		System.out.println(json.encodePrettily());
		assertThat(json).compliesToAssertions(queryName);
	}

	@Test
	public void testReadProjectNoPerm() throws Throwable {
		try (Tx tx = tx()) {
			role().revokePermissions(project(), READ_PERM);
			tx.success();
		}
		call(() -> client().graphqlQuery(PROJECT_NAME, "{project{name}}"));
	}

}
