package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
@RunWith(Parameterized.class)
public class GraphQLPermissionTest extends AbstractMeshTest {
	/**
	 * Provide test variations
	 * @return test variations
	 */
	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> paramData() {
		return Arrays.asList(new Object[][] { { "draft" }, { "published" } });
	}

	@Parameter(0)
	public String version;

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
			RoleDao roleDao = tx.roleDao();
			NodeDao nodeDao = tx.nodeDao();
			for (HibNode node : nodeDao.findAll(project())) {
				roleDao.revokePermissions(role(), node, InternalPermission.READ_PERM);
			}
			// Explicitly remove read_publish for a single node
			roleDao.revokePermissions(role(), folder("news"), InternalPermission.READ_PUBLISHED_PERM);
			tx.success();
		}

		String queryName2 = "node-perm-children-query." + version;
		GraphQLResponse response2 = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName2), new VersioningParametersImpl().setVersion(version)));
		JsonObject json2 = new JsonObject(response2.toJson());
		System.out.println(json2.encodePrettily());
		assertThat(json2).compliesToAssertions(queryName2);
	}

}
