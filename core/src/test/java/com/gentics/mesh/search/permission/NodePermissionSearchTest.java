package com.gentics.mesh.search.permission;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.PROJECT_AND_NODE, startServer = true, startESServer = true)
public class NodePermissionSearchTest extends AbstractMeshTest {

	@Test
	public void testIndexPermUpdate() throws Exception {
		NodeResponse response = createNode("slug", FieldUtil.createStringField("slugblub"));

		String json = getESQuery("nodeWildcard.es");

		NodeListResponse list = call(() -> client().searchNodes(PROJECT_NAME, json));
		assertEquals("The user should be found since the requestor has permission to see it", 1, list.getData().size());

		// Revoke read permission
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes/" + response.getUuid(), request));

		list = call(() -> client().searchNodes(PROJECT_NAME, json));
		assertEquals("The user should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}

	@Test
	public void testIndexPermRoleDeletion() throws IOException {
		NodeResponse response = createNode("slug", FieldUtil.createStringField("slugblub"));

		String json = getESQuery("nodeWildcard.es");

		NodeListResponse list = call(() -> client().searchNodes(PROJECT_NAME, json));
		assertEquals("The user should be found since the requestor has permission to see it", 1, list.getData().size());

		// Delete the role
		call(() -> client().deleteRole(roleUuid()));

		list = call(() -> client().searchNodes(PROJECT_NAME, json));
		assertEquals("The user should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}
}
