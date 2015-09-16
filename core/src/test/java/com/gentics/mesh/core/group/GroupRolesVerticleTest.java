package com.gentics.mesh.core.group;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class GroupRolesVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle verticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	// Group Role Testcases - PUT / Add

	@Test
	public void testReadRolesByGroup() throws Exception {
		RoleRoot root = meshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole", group(), user());
		String roleUuid = extraRole.getUuid();
		role().grantPermissions(extraRole, READ_PERM);
		String groupUuid = group().getUuid();

		Future<RoleListResponse> future = getClient().findRolesForGroup(groupUuid);
		latchFor(future);
		assertSuccess(future);
		RoleListResponse roleList = future.result();
		assertEquals(2, roleList.getMetainfo().getTotalCount());
		assertEquals(2, roleList.getData().size());

		Set<String> listedRoleUuids = new HashSet<>();
		for (RoleResponse role : roleList.getData()) {
			listedRoleUuids.add(role.getUuid());
		}

		assertTrue(listedRoleUuids.contains(role().getUuid()));
		assertTrue(listedRoleUuids.contains(roleUuid));
	}

	@Test
	public void testAddRoleToGroup() throws Exception {
		String roleUuid;
		String groupUuid;
		RoleRoot root = meshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole", null, user());
		roleUuid = extraRole.getUuid();
		role().grantPermissions(extraRole, READ_PERM);
		assertEquals(1, group().getRoles().size());
		groupUuid = group().getUuid();

		Future<GroupResponse> future = getClient().addRoleToGroup(groupUuid, roleUuid);
		latchFor(future);
		assertSuccess(future);
		GroupResponse restGroup = future.result();
		assertTrue(restGroup.getRoles().contains("extraRole"));

		Group group = group();
		assertEquals(2, group.getRoles().size());
	}

	@Test
	public void testAddBogusRoleToGroup() throws Exception {
		String uuid;
		assertEquals(1, group().getRoles().size());
		uuid = group().getUuid();

		Future<GroupResponse> future = getClient().addRoleToGroup(uuid, "bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testAddNoPermissionRoleToGroup() throws Exception {
		String roleUuid;
		String groupUuid;
		RoleRoot root = meshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole", null, user());
		roleUuid = extraRole.getUuid();
		assertEquals(1, group().getRoles().size());
		groupUuid = group().getUuid();

		Future<GroupResponse> future = getClient().addRoleToGroup(groupUuid, roleUuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", roleUuid);

		Group group = group();
		assertEquals(1, group.getRoles().size());
	}

	@Test
	public void testRemoveRoleFromGroup() throws Exception {
		String groupUuid;
		String roleUuid;
		RoleRoot root = meshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole", null, user());
		roleUuid = extraRole.getUuid();
		group().addRole(extraRole);
		role().grantPermissions(extraRole, READ_PERM);
		assertEquals(2, group().getRoles().size());
		groupUuid = group().getUuid();

		Future<GroupResponse> future = getClient().removeRoleFromGroup(groupUuid, roleUuid);
		latchFor(future);
		assertSuccess(future);
		GroupResponse restGroup = future.result();
		assertFalse(restGroup.getRoles().contains("extraRole"));

		Group group = group();
		assertEquals(1, group.getRoles().size());

	}

	@Test
	public void testAddRoleToGroupWithPerm() throws Exception {
		Role extraRole;
		RoleRoot root = meshRoot().getRoleRoot();

		extraRole = root.create("extraRole", null, user());
		role().grantPermissions(extraRole, READ_PERM);
		Future<GroupResponse> future = getClient().addRoleToGroup(group().getUuid(), extraRole.getUuid());
		latchFor(future);
		assertSuccess(future);
		GroupResponse restGroup = future.result();
		test.assertGroup(group(), restGroup);

		assertTrue("Role should be assigned to group.", group().hasRole(extraRole));
	}

	@Test
	public void testAddRoleToGroupWithoutPermOnGroup() throws Exception {
		Role extraRole;
		Group group = group();
		RoleRoot root = meshRoot().getRoleRoot();
		extraRole = root.create("extraRole", null, user());
		role().revokePermissions(group, UPDATE_PERM);
		Future<GroupResponse> future = getClient().addRoleToGroup(group().getUuid(), extraRole.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", group().getUuid());
		assertFalse("Role should not be assigned to group.", group().hasRole(extraRole));
	}

	@Test
	public void testAddRoleToGroupWithBogusRoleUUID() throws Exception {

		Future<GroupResponse> future = getClient().addRoleToGroup(group().getUuid(), "bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	// Group Role Testcases - DELETE / Remove

	@Test
	public void testRemoveRoleFromGroupWithPerm() throws Exception {
		RoleRoot root = meshRoot().getRoleRoot();
		Group group = group();
		Role extraRole = root.create("extraRole", group, user());

		assertNotNull(group.getUuid());
		assertNotNull(extraRole.getUuid());

		role().grantPermissions(extraRole, READ_PERM);
		role().grantPermissions(group, UPDATE_PERM);

		Future<GroupResponse> future;
		future = getClient().removeRoleFromGroup(group().getUuid(), extraRole.getUuid());
		latchFor(future);
		assertSuccess(future);

		GroupResponse restGroup = future.result();
		test.assertGroup(group(), restGroup);
		assertFalse("Role should now no longer be assigned to group.", group().hasRole(extraRole));
	}

	@Test
	public void testRemoveRoleFromGroupWithoutPerm() throws Exception {
		Group group = group();
		RoleRoot root = meshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole", group, user());
		role().revokePermissions(group, UPDATE_PERM);

		Future<GroupResponse> future = getClient().removeRoleFromGroup(group().getUuid(), extraRole.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", group().getUuid());
		assertTrue("Role should be stil assigned to group.", group().hasRole(extraRole));
	}
}
