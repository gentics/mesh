package com.gentics.mesh.core.perm;

import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.context.impl.DummyEventQueueBatch;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

/**
 * Test cases for usage and clearing of PermissionCache
 */
@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true)
public class PermissionCacheTest extends AbstractMeshTest {
	/**
	 * Before every test, the cache should be empty, and the user should have full permissions on the element
	 * After checking the permissions, the cache should no longer be empty
	 */
	@Before
	public void assertCacheEmpty() {
		assertPermissions("initialization", InternalPermission.CREATE_PERM, InternalPermission.READ_PERM,
				InternalPermission.UPDATE_PERM, InternalPermission.DELETE_PERM);
	}

	/**
	 * Test revoking and granting permissions on an element (for a role)
	 */
	@Test
	public void testChangePermission() {
		// revoke delete and update permissions
		db().tx(tx -> {
			HibProject project = tx.projectDao().findByUuid(projectUuid());
			HibRole role = tx.roleDao().findByUuid(roleUuid());
			tx.roleDao().applyPermissions(project, new DummyEventQueueBatch(), role, false, Collections.emptySet(),
					new HashSet<>(Arrays.asList(InternalPermission.DELETE_PERM, InternalPermission.UPDATE_PERM)));
		});

		assertPermissions("revoking permissions", InternalPermission.CREATE_PERM, InternalPermission.READ_PERM);

		// grant update permission
		db().tx(tx -> {
			HibProject project = tx.projectDao().findByUuid(projectUuid());
			HibRole role = tx.roleDao().findByUuid(roleUuid());
			tx.roleDao().applyPermissions(project, new DummyEventQueueBatch(), role, false,
					new HashSet<>(Arrays.asList(InternalPermission.UPDATE_PERM)), Collections.emptySet());
		});

		assertPermissions("granting permissions", InternalPermission.CREATE_PERM, InternalPermission.READ_PERM,
				InternalPermission.UPDATE_PERM);
	}

	/**
	 * Test removing a user from a group and adding a user to a group
	 */
	@Test
	public void testUserGroupAssignment() {
		// remove user from group
		db().tx(tx -> {
			HibGroup group = tx.groupDao().findByUuid(groupUuid());
			HibUser user = tx.userDao().findByUuid(userUuid());
			tx.groupDao().removeUser(group, user);
		});

		assertPermissions("removing user from group");

		// add user to group
		db().tx(tx -> {
			HibGroup group = tx.groupDao().findByUuid(groupUuid());
			HibUser user = tx.userDao().findByUuid(userUuid());
			tx.groupDao().addUser(group, user);
		});

		assertPermissions("adding user to group", InternalPermission.CREATE_PERM, InternalPermission.READ_PERM,
				InternalPermission.UPDATE_PERM, InternalPermission.DELETE_PERM);
	}

	/**
	 * Test removing a role from a group and adding a role to a group
	 */
	@Test
	public void testGroupRoleAssignment() {
		// remove role from group
		db().tx(tx -> {
			HibGroup group = tx.groupDao().findByUuid(groupUuid());
			HibRole role = tx.roleDao().findByUuid(roleUuid());
			tx.groupDao().removeRole(group, role);
		});

		assertPermissions("removing role from group");

		// add role to group
		db().tx(tx -> {
			HibGroup group = tx.groupDao().findByUuid(groupUuid());
			HibRole role = tx.roleDao().findByUuid(roleUuid());
			tx.groupDao().addRole(group, role);
		});

		assertPermissions("adding role to group", InternalPermission.CREATE_PERM, InternalPermission.READ_PERM,
				InternalPermission.UPDATE_PERM, InternalPermission.DELETE_PERM);
	}

	/**
	 * Test deleting a role
	 */
	@Test
	public void testDeleteRole() {
		// delete the role
		db().tx(tx -> {
			HibRole role = tx.roleDao().findByUuid(roleUuid());
			tx.roleDao().delete(role, new DummyBulkActionContext());
		});

		assertPermissions("deleting role");
	}

	/**
	 * Test deleting a group
	 */
	@Test
	public void testDeleteGroup() {
		// delete the group
		db().tx(tx -> {
			HibGroup group = tx.groupDao().findByUuid(groupUuid());
			tx.groupDao().delete(group, new DummyBulkActionContext());
		});

		assertPermissions("deleting group");
	}

	/**
	 * Test deleting the user
	 */
	@Test
	public void testDeleteUser() {
		String userUuid = db().tx(tx -> {
			return tx.userDao().create("blub", user()).getUuid();
		});
		assertThat(getPermissionCacheSize()).as("Cache size before deleting user").isEqualTo(1);
		db().tx(tx -> {
			HibUser user = tx.userDao().findByUuid(userUuid);
			tx.userDao().delete(user, new DummyBulkActionContext());
		});
		assertThat(getPermissionCacheSize()).as("Cache size after deleting user").isEqualTo(0);
	}

	/**
	 * Test granting admin permission
	 */
	@Test
	public void testGrantAdmin() {
		// revoke all permissions from the group and revoke admin flag from user
		db().tx(tx -> {
			HibProject project = tx.projectDao().findByUuid(projectUuid());
			HibRole role = tx.roleDao().findByUuid(roleUuid());
			tx.roleDao().applyPermissions(project, new DummyEventQueueBatch(), role, false, Collections.emptySet(),
					new HashSet<>(Arrays.asList(InternalPermission.CREATE_PERM, InternalPermission.READ_PERM,
							InternalPermission.UPDATE_PERM, InternalPermission.DELETE_PERM)));
		});

		revokeAdmin();
		db().tx(tx -> {
			tx.permissionCache().clear();
		});
		assertPermissions("clearing permissions and revoking admin flag");

		// grant the admin flag by using the method UserDao.update, to check whether this clears the permission cache
		db().tx(tx -> {
			HibUser user = tx.userDao().findByUuid(userUuid());
			HibUser admin = tx.userDao().findByUsername("admin");
			UserUpdateRequest request = new UserUpdateRequest();
			request.setAdmin(true);
			InternalActionContext ac = getMockedInternalActionContext("", admin, project(), request);
			tx.userDao().update(user, ac, new DummyEventQueueBatch());
		});
		assertPermissions("granting admin flag", InternalPermission.CREATE_PERM, InternalPermission.READ_PERM,
				InternalPermission.UPDATE_PERM, InternalPermission.DELETE_PERM);
	}

	/**
	 * Test revoking admin permission
	 */
	@Test
	public void testRevokeAdmin() {
		// revoke all permissions from the group and grant admin flag from user
		db().tx(tx -> {
			HibProject project = tx.projectDao().findByUuid(projectUuid());
			HibRole role = tx.roleDao().findByUuid(roleUuid());
			tx.roleDao().applyPermissions(project, new DummyEventQueueBatch(), role, false, Collections.emptySet(),
					new HashSet<>(Arrays.asList(InternalPermission.CREATE_PERM, InternalPermission.READ_PERM,
							InternalPermission.UPDATE_PERM, InternalPermission.DELETE_PERM)));
		});
		grantAdmin();
		db().tx(tx -> {
			tx.permissionCache().clear();
		});
		assertPermissions("clearing permissions and setting admin flag", InternalPermission.CREATE_PERM,
				InternalPermission.READ_PERM, InternalPermission.UPDATE_PERM, InternalPermission.DELETE_PERM);

		// revoke the admin flag by using the method UserDao.update, to check whether this clears the permission cache
		db().tx(tx -> {
			HibUser user = tx.userDao().findByUuid(userUuid());
			HibUser admin = tx.userDao().findByUsername("admin");
			UserUpdateRequest request = new UserUpdateRequest();
			request.setAdmin(false);
			InternalActionContext ac = getMockedInternalActionContext("", admin, project(), request);
			tx.userDao().update(user, ac, new DummyEventQueueBatch());
		});
		assertPermissions("revoking admin flag");
	}

	/**
	 * Get the size of the permission cache
	 * @return cache size
	 */
	protected long getPermissionCacheSize() {
		return db().tx(tx -> {
			return tx.permissionCache().size();
		});
	}

	/**
	 * Get the permissions for the user on the project.
	 * Invoking this method will get the permission from the cache (if cached) or will get the permissions
	 * from the DB and put them into the cache
	 * @return current permissions of the user
	 */
	protected Set<InternalPermission> getPermissionsOnProject() {
		return db().tx(tx -> {
			HibProject project = tx.projectDao().findByUuid(projectUuid());
			HibUser user = tx.userDao().findByUuid(userUuid());
			return tx.userDao().getPermissions(user, project);
		});
	}

	/**
	 * Assert that
	 * <ol>
	 * <li>Permission cache is empty before checking the permissions</li>
	 * <li>The user has exactly the given permissions on the project</li>
	 * <li>Permission cache is no longer empty after checking the permissions</li>
	 * </ol>
	 * @param state "state" of the test procedure (e.g. what was the last action performed)
	 * @param perms expected permissions
	 */
	protected void assertPermissions(String state, InternalPermission...perms) {
		// cache is supposed to be cleared
		assertThat(getPermissionCacheSize()).as("Cache size after " + state).isEqualTo(0);

		// get permissions
		assertThat(getPermissionsOnProject()).as("Permissions after " + state).containsOnly(perms);
		assertThat(getPermissionCacheSize()).as("Cache size after " + state).isNotEqualTo(0);
	}
}
