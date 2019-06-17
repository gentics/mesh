package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
@RunWith(Parameterized.class)
public class GraphQLPermissionTest extends AbstractMeshTest {

	private final String queryName;

	public GraphQLPermissionTest(String queryName) {
		this.queryName = queryName;
	}

	@Parameters(name = "query={0}")
	public static Collection<Object[]> paramData() {
		Collection<Object[]> testData = new Vector<>();
		testData.add(new Object[] { "node-perm-children-query" });
		return testData;
	}

	@Test
	public void testReadPublishedNodeChildren() throws IOException {

		// 1. Publish all nodes
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());
		call(() -> client().publishNode(PROJECT_NAME, baseNodeUuid, new PublishParametersImpl().setRecursive(true)));

		// 2. Revoke all read perm from all nodes also read_published from /News
		try (Tx tx = tx()) {
			for (Node node : project().getNodeRoot().findAll()) {
				role().revokePermissions(node, GraphPermission.READ_PERM);
			}
			// Explicitly remove read_publish for a single node
			role().revokePermissions(folder("news"), GraphPermission.READ_PUBLISHED_PERM);
			tx.success();
		}

		// 3. Invoke the query and assert that the nodes can still be loaded (due to read published)
		GraphQLResponse response = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName), new VersioningParametersImpl().setVersion("published")));
		JsonObject json = new JsonObject(response.toJson());
		System.out.println(json.encodePrettily());
		assertThat(json).compliesToAssertions(queryName);
	}

}
