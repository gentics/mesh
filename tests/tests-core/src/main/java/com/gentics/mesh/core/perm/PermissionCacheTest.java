package com.gentics.mesh.core.perm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.context.impl.DummyEventQueueBatch;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
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
		assertPermissions("Initial", InternalPermission.CREATE_PERM, InternalPermission.READ_PERM,
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

		assertPermissions("Revoking permissions", InternalPermission.CREATE_PERM, InternalPermission.READ_PERM);

		// grant update permission
		db().tx(tx -> {
			HibProject project = tx.projectDao().findByUuid(projectUuid());
			HibRole role = tx.roleDao().findByUuid(roleUuid());
			tx.roleDao().applyPermissions(project, new DummyEventQueueBatch(), role, false,
					new HashSet<>(Arrays.asList(InternalPermission.UPDATE_PERM)), Collections.emptySet());
		});

		assertPermissions("Granting permissions", InternalPermission.CREATE_PERM, InternalPermission.READ_PERM,
				InternalPermission.UPDATE_PERM);
	}

	@Test
	public void testUserGroupAssignment() {
		// remove user from group
		db().tx(tx -> {
			HibGroup group = tx.groupDao().findByUuid(groupUuid());
			HibUser user = tx.userDao().findByUuid(userUuid());
			tx.groupDao().removeUser(group, user);
		});

		assertPermissions("Remove user from group");

		// add user to group
		db().tx(tx -> {
			HibGroup group = tx.groupDao().findByUuid(groupUuid());
			HibUser user = tx.userDao().findByUuid(userUuid());
			tx.groupDao().addUser(group, user);
		});

		assertPermissions("Add user to group", InternalPermission.CREATE_PERM, InternalPermission.READ_PERM,
				InternalPermission.UPDATE_PERM, InternalPermission.DELETE_PERM);
	}

	@Test
	public void testGroupRoleAssignment() {
		// remove role from group
		db().tx(tx -> {
			HibGroup group = tx.groupDao().findByUuid(groupUuid());
			HibRole role = tx.roleDao().findByUuid(roleUuid());
			tx.groupDao().removeRole(group, role);
		});

		assertPermissions("Remove role from group");

		// add role to group
		db().tx(tx -> {
			HibGroup group = tx.groupDao().findByUuid(groupUuid());
			HibRole role = tx.roleDao().findByUuid(roleUuid());
			tx.groupDao().addRole(group, role);
		});

		assertPermissions("Add role to group", InternalPermission.CREATE_PERM, InternalPermission.READ_PERM,
				InternalPermission.UPDATE_PERM, InternalPermission.DELETE_PERM);
	}

	@Test
	public void testDeleteRole() {
		// delete the role
		db().tx(tx -> {
			HibRole role = tx.roleDao().findByUuid(roleUuid());
			tx.roleDao().delete(role, new DummyBulkActionContext());
		});

		assertPermissions("Delete role");
	}

	@Test
	public void testDeleteGroup() {
		// delete the group
		db().tx(tx -> {
			HibGroup group = tx.groupDao().findByUuid(groupUuid());
			tx.groupDao().delete(group, new DummyBulkActionContext());
		});

		assertPermissions("Delete group");
	}

	protected long getPermissionCacheSize() {
		return db().tx(tx -> {
			return tx.permissionCache().size();
		});
	}

	protected Set<InternalPermission> getPermissionsOnProject() {
		return db().tx(tx -> {
			HibProject project = tx.projectDao().findByUuid(projectUuid());
			HibUser user = tx.userDao().findByUuid(userUuid());
			return tx.userDao().getPermissions(user, project);
		});
	}

	protected void assertPermissions(String state, InternalPermission...perms) {
		// cache is supposed to be cleared
		assertThat(getPermissionCacheSize()).as("Cache size after " + state).isEqualTo(0);

		// get permissions
		assertThat(getPermissionsOnProject()).as("Permissions after " + state).containsOnly(perms);
		assertThat(getPermissionCacheSize()).as("Cache size after " + state).isNotEqualTo(0);
	}
}
