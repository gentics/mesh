package com.gentics.mesh.search.permission;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.PROJECT_AND_NODE, startServer = true, startESServer = false)
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

		try (Tx tx = tx()) {
			recreateIndices();
		}

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

		try (Tx tx = tx()) {
			recreateIndices();
		}

		list = call(() -> client().searchGroups(json));
		assertEquals("The group should be found since we added the permission to see it", 1, list.getData().size());

	}

	@Test
	public void testIndexPermUpdate() throws Exception {
		String groupname = "testgroup42a";
		GroupResponse response = createGroup(groupname);

		String json = getESText("groupWildcard.es");

		GroupListResponse list = call(() -> client().searchGroups(json));
		assertEquals("The group should be found since the requestor has permission to see it", 1, list.getData().size());

		// Revoke read permission
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/groups/" + response.getUuid(), request));

		list = call(() -> client().searchGroups(json));
		assertEquals("The group should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}

}
