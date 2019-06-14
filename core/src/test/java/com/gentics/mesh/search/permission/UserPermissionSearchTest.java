package com.gentics.mesh.search.permission;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = CONTAINER, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class UserPermissionSearchTest extends AbstractMeshTest {

	@Test
	public void testReadPermHandling() throws Exception {

		String username = "testuser42a";
		UserResponse response = createUser(username);
		try (Tx tx = tx()) {
			User user = meshRoot().getUserRoot().findByUuid(response.getUuid());
			System.out.println("User Uuid:" + response.getUuid());
			for (Role role : user().getRoles()) {
				role.revokePermissions(user, GraphPermission.READ_PERM);
			}
			tx.success();
		}

		try (Tx tx = tx()) {
			recreateIndices();
		}

		String json = getESText("userWildcard.es");

		UserListResponse list = call(() -> client().searchUsers(json));
		assertEquals("The user should not be found since the requestor has no permission to see it", 0, list.getData().size());

		// Now add the perm
		try (Tx tx = tx()) {
			User user = meshRoot().getUserRoot().findByUuid(response.getUuid());
			System.out.println("User Uuid:" + response.getUuid());
			role().grantPermissions(user, GraphPermission.READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			recreateIndices();
		}

		list = call(() -> client().searchUsers(json));
		assertEquals("The user should be found since we added the permission to see it", 1, list.getData().size());

	}

	@Test
	public void testIndexPermUpdate() throws Exception {
		String username = "testuser42a";
		UserResponse response = createUser(username);
		waitForSearchIdleEvent();

		String json = getESText("userWildcard.es");

		UserListResponse list = call(() -> client().searchUsers(json));
		assertEquals("The user should be found since the requestor has permission to see it", 1, list.getData().size());

		// Revoke read permission
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/users/" + response.getUuid(), request));
		waitForSearchIdleEvent();

		list = call(() -> client().searchUsers(json));
		assertEquals("The user should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}

}
