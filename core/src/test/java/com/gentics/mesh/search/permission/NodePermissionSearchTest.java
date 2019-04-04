package com.gentics.mesh.search.permission;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;
@MeshTestSetting(elasticsearch = CONTAINER, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class NodePermissionSearchTest extends AbstractMeshTest {

	@Test
	public void testIndexPermUpdate() throws Exception {
		NodeResponse response = createNode("slug", FieldUtil.createStringField("slugblub"));

		String json = getESText("nodeWildcard.es");

		waitForSearchIdleEvent();
		NodeListResponse list = call(() -> client().searchNodes(PROJECT_NAME, json));
		assertEquals("The node should be found since the requestor has permission to see it", 1, list.getData().size());

		// Revoke read permission
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes/" + response.getUuid(), request));

		waitForSearchIdleEvent();
		list = call(() -> client().searchNodes(PROJECT_NAME, json));
		assertEquals("The node should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}

	@Test
	public void testIndexPermRoleDeletion() throws Exception {
		recreateIndices();
		createNode("slug", FieldUtil.createStringField("slugblub"));

		String json = getESText("nodeWildcard.es");

		waitForSearchIdleEvent();
		NodeListResponse list = call(() -> client().searchNodes(PROJECT_NAME, json));
		assertEquals("The node should be found since the requestor has permission to see it", 1, list.getData().size());

		// Delete the role
		call(() -> client().deleteRole(roleUuid()));

		waitForSearchIdleEvent();
		list = call(() -> client().searchNodes(PROJECT_NAME, json));
		assertEquals("The node should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}

	@Test
	public void testReadPublishPerm() throws Exception {
		recreateIndices();
		NodeResponse response = createNode("slug", FieldUtil.createStringField("slugblub"));
		call(() -> client().publishNode(PROJECT_NAME, response.getUuid()));

		String json = getESText("nodeWildcard.es");

		waitForSearchIdleEvent();

		NodeListResponse list = call(() -> client().searchNodes(PROJECT_NAME, json, new VersioningParametersImpl().published()));
		assertEquals("The node should be found since the requestor has permission to see it", 1, list.getData().size());

		// Revoke read permission and only grant read published
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		request.getPermissions().setReadPublished(true);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes/" + response.getUuid(), request));

		waitForSearchIdleEvent();

		list = call(() -> client().searchNodes(PROJECT_NAME, json, new VersioningParametersImpl().published()));
		assertEquals("The node should be found since the requestor has permission read publish", 1, list.getData().size());

		request.getPermissions().setReadPublished(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes/" + response.getUuid(), request));

		waitForSearchIdleEvent();

		list = call(() -> client().searchNodes(PROJECT_NAME, json, new VersioningParametersImpl().published()));
		assertEquals("The node should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}

	/**
	 * Verify that the permission handling works correct when deleting roles which only grant read perm on nodes.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRoleDeletion() throws Exception {
		recreateIndices();
		NodeResponse response = createNode("slug", FieldUtil.createStringField("slugblub"));
		call(() -> client().publishNode(PROJECT_NAME, response.getUuid()));

		String json = getESText("nodeWildcard.es");
		waitForSearchIdleEvent();
		NodeListResponse list = call(() -> client().searchNodes(PROJECT_NAME, json, new VersioningParametersImpl().published()));
		assertEquals("The node should be found since the requestor has permission to see it", 1, list.getData().size());

		// Create a role which only grant read published perm
		RoleResponse roleResponse = call(() -> client().createRole(new RoleCreateRequest().setName("ReadpubPermRole")));
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		request.getPermissions().setReadPublished(true);
		call(() -> client().updateRolePermissions(roleResponse.getUuid(), "/projects/" + PROJECT_NAME + "/nodes/" + response.getUuid(), request));
		call(() -> client().addRoleToGroup(groupUuid(), roleResponse.getUuid()));

		// Revoke read perm from own role
		RolePermissionRequest request2 = new RolePermissionRequest();
		request2.getPermissions().setRead(false);
		request2.getPermissions().setReadPublished(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes/" + response.getUuid(), request2));

		waitForSearchIdleEvent();
		list = call(() -> client().searchNodes(PROJECT_NAME, json, new VersioningParametersImpl().published()));
		assertEquals("The node should be found since the requestor has permission read publish", 1, list.getData().size());

		// Delete the role
		call(() -> client().deleteRole(roleResponse.getUuid()));

		waitForSearchIdleEvent();

		list = call(() -> client().searchNodes(PROJECT_NAME, json, new VersioningParametersImpl().published()));
		assertEquals("The node should not be found since the requestor has no permission to see it", 0, list.getData().size());

		// Now recreate the role with the same uuid
		RoleResponse roleResponse2 = call(() -> client().createRole(roleResponse.getUuid(), new RoleCreateRequest().setName("ReadpubPermRole2")));
		call(() -> client().addRoleToGroup(groupUuid(), roleResponse2.getUuid()));

		waitForSearchIdleEvent();

		// Search again - The node should still not be visible since the new role has no permissions
		list = call(() -> client().searchNodes(PROJECT_NAME, json, new VersioningParametersImpl().published()));
		assertEquals("The node should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}
}
