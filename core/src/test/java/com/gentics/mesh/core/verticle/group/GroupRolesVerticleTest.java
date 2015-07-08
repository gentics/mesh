package com.gentics.mesh.core.verticle.group;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.Future;

import java.util.Iterator;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.verticle.GroupVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class GroupRolesVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle groupsVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return groupsVerticle;
	}

	// Group Role Testcases - PUT / Add

	@Test
	public void testReadRolesByGroup() throws Exception {
		RoleRoot root = data().getMeshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole");

		info.getGroup().addRole(extraRole);
		info.getRole().addPermissions(extraRole, READ_PERM);

		String uuid = info.getGroup().getUuid();
		Future<RoleListResponse> future = getClient().findRolesForGroup(uuid);
		latchFor(future);
		assertSuccess(future);
		RoleListResponse roleList = future.result();
		assertEquals(2, roleList.getMetainfo().getTotalCount());
		assertEquals(2, roleList.getData().size());

		Iterator<RoleResponse> roleIt = roleList.getData().iterator();
		RoleResponse roleB = roleIt.next();
		RoleResponse roleA = roleIt.next();
		assertEquals(info.getRole().getUuid(), roleB.getUuid());
		assertEquals(extraRole.getUuid(), roleA.getUuid());
	}

	@Test
	public void testAddRoleToGroup() throws Exception {
		RoleRoot root = data().getMeshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole");

		info.getRole().addPermissions(extraRole, READ_PERM);

		assertEquals(1, info.getGroup().getRoles().size());
		String uuid = info.getGroup().getUuid();
		Future<GroupResponse> future = getClient().addRoleToGroup(uuid, extraRole.getUuid());
		latchFor(future);
		assertSuccess(future);
		GroupResponse restGroup = future.result();
		assertTrue(restGroup.getRoles().contains("extraRole"));

		Group group = info.getGroup();
		assertEquals(2, group.getRoles().size());

	}

	@Test
	public void testAddBogusRoleToGroup() throws Exception {
		assertEquals(1, info.getGroup().getRoles().size());
		String uuid = info.getGroup().getUuid();
		Future<GroupResponse> future = getClient().addRoleToGroup(uuid, "bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testAddNoPermissionRoleToGroup() throws Exception {
		RoleRoot root = data().getMeshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole");

		assertEquals(1, info.getGroup().getRoles().size());
		String uuid = info.getGroup().getUuid();
		Future<GroupResponse> future = getClient().addRoleToGroup(uuid, extraRole.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", extraRole.getUuid());

		Group group = info.getGroup();
		assertEquals(1, group.getRoles().size());
	}

	@Test
	public void testRemoveRoleFromGroup() throws Exception {
		RoleRoot root = data().getMeshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole");

		info.getGroup().addRole(extraRole);
		info.getRole().addPermissions(extraRole, READ_PERM);
		assertEquals(2, info.getGroup().getRoles().size());
		String uuid = info.getGroup().getUuid();

		Future<GroupResponse> future = getClient().removeRoleFromGroup(uuid, extraRole.getUuid());
		latchFor(future);
		assertSuccess(future);
		GroupResponse restGroup = future.result();
		assertFalse(restGroup.getRoles().contains("extraRole"));
		Group group = info.getGroup();
		assertEquals(1, group.getRoles().size());

	}

	@Test
	public void testAddRoleToGroupWithPerm() throws Exception {
		Group group = info.getGroup();
		RoleRoot root = data().getMeshRoot().getRoleRoot();

		Role extraRole = root.create("extraRole");
		info.getRole().addPermissions(extraRole, READ_PERM);

		Future<GroupResponse> future = getClient().addRoleToGroup(group.getUuid(), extraRole.getUuid());
		latchFor(future);
		assertSuccess(future);
		GroupResponse restGroup = future.result();
		test.assertGroup(group, restGroup);

		assertTrue("Role should be assigned to group.", group.hasRole(extraRole));
	}

	@Test
	public void testAddRoleToGroupWithoutPermOnGroup() throws Exception {
		Group group = info.getGroup();
		RoleRoot root = data().getMeshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole");
		info.getRole().revokePermissions(group, UPDATE_PERM);

		Future<GroupResponse> future = getClient().addRoleToGroup(group.getUuid(), extraRole.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", group.getUuid());
		assertFalse("Role should not be assigned to group.", group.hasRole(extraRole));
	}

	@Test
	public void testAddRoleToGroupWithBogusRoleUUID() throws Exception {
		Group group = info.getGroup();
		Future<GroupResponse> future = getClient().addRoleToGroup(group.getUuid(), "bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	// Group Role Testcases - DELETE / Remove

	@Test
	public void testRemoveRoleFromGroupWithPerm() throws Exception {
		RoleRoot root = data().getMeshRoot().getRoleRoot();
		Group group = info.getGroup();

		Role extraRole = root.create("extraRole");
		group.addRole(extraRole);

		assertNotNull(group.getUuid());
		assertNotNull(extraRole.getUuid());

		info.getRole().addPermissions(extraRole, READ_PERM);
		info.getRole().addPermissions(group, UPDATE_PERM);

		Future<GroupResponse> future = getClient().removeRoleFromGroup(group.getUuid(), extraRole.getUuid());
		latchFor(future);
		assertSuccess(future);
		GroupResponse restGroup = future.result();
		test.assertGroup(group, restGroup);
		assertFalse("Role should now no longer be assigned to group.", group.hasRole(extraRole));
	}

	@Test
	public void testRemoveRoleFromGroupWithoutPerm() throws Exception {
		Group group = info.getGroup();
		RoleRoot root = data().getMeshRoot().getRoleRoot();

		Role extraRole = root.create("extraRole");
		group.addRole(extraRole);
		info.getRole().revokePermissions(group, UPDATE_PERM);

		Future<GroupResponse> future = getClient().removeRoleFromGroup(group.getUuid(), extraRole.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", group.getUuid());
		assertTrue("Role should be stil assigned to group.", group.hasRole(extraRole));
	}
}
