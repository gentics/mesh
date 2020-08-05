package com.gentics.mesh.search.permission;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class GroupPermissionSearchTest extends AbstractMeshTest {

	@Test
	public void testReadPermHandling() throws Exception {

		String groupname = "testgroup42a";
		GroupResponse response = createGroup(groupname);
		try (Tx tx = tx()) {
			Group group = meshRoot().getGroupRoot().findByUuid(response.getUuid());
			System.out.println("Group Uuid:" + response.getUuid());
			for (Role role : user().getRoles()) {
				role.revokePermissions(group, GraphPermission.READ_PERM);
			}
			tx.success();
		}

		recreateIndices();

		String json = getESText("groupWildcard.es");

		GroupListResponse list = call(() -> client().searchGroups(json));
		assertEquals("The group should not be found since the requestor has no permission to see it", 0, list.getData().size());

		// Now add the perm
		try (Tx tx = tx()) {
			Group group = meshRoot().getGroupRoot().findByUuid(response.getUuid());
			System.out.println("Group Uuid:" + response.getUuid());
			role().grantPermissions(group, GraphPermission.READ_PERM);
			tx.success();
		}

		recreateIndices();

		list = call(() -> client().searchGroups(json));
		assertEquals("The group should be found since we added the permission to see it", 1, list.getData().size());

	}

	@Test
	public void testIndexPermUpdate() throws Exception {
		String groupname = "testgroup42a";
		GroupResponse response = createGroup(groupname);

		String json = getESText("groupWildcard.es");

		waitForSearchIdleEvent();

		GroupListResponse list = call(() -> client().searchGroups(json));
		assertEquals("The group should be found since the requestor has permission to see it", 1, list.getData().size());

		// Revoke read permission
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/groups/" + response.getUuid(), request));

		waitForSearchIdleEvent();

		list = call(() -> client().searchGroups(json));
		assertEquals("The group should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}

}
