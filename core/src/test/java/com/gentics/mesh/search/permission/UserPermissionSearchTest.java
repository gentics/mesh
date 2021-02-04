package com.gentics.mesh.search.permission;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.dao.OrientDBRoleDao;
import com.gentics.mesh.core.data.dao.OrientDBUserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class UserPermissionSearchTest extends AbstractMeshTest {

	@Test
	public void testReadPermHandling() throws Exception {

		String username = "testuser42a";
		UserResponse response = createUser(username);
		try (Tx tx = tx()) {
			OrientDBRoleDao roleDao = tx.roleDao();
			OrientDBUserDao userDao = tx.userDao();
			HibUser user = meshRoot().getUserRoot().findByUuid(response.getUuid());
			System.out.println("User Uuid:" + response.getUuid());
			for (HibRole role : userDao.getRoles(user())) {
				roleDao.revokePermissions(role, user, InternalPermission.READ_PERM);
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
			OrientDBRoleDao roleDao = tx.roleDao();
			OrientDBUserDao userDao = tx.userDao();
			HibUser user = userDao.findByUuid(response.getUuid());
			System.out.println("User Uuid:" + response.getUuid());
			roleDao.grantPermissions(role(), user, InternalPermission.READ_PERM);
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
