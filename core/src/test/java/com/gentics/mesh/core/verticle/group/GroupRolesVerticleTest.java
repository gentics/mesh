package com.gentics.mesh.core.verticle.group;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.verticle.GroupVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;
public class GroupRolesVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle groupsVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return groupsVerticle;
	}

	// Group Role Testcases - PUT / Add

	@Test
	public void testReadRolesByGroup() throws Exception {
		String groupUuid;
		String roleUuid;
		try (Trx tx = new Trx(db)) {
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", group(), user());
			roleUuid = extraRole.getUuid();
			role().grantPermissions(extraRole, READ_PERM);
			groupUuid = group().getUuid();
		}

		Future<RoleListResponse> future = getClient().findRolesForGroup(groupUuid);
		latchFor(future);
		assertSuccess(future);
		RoleListResponse roleList = future.result();
		assertEquals(2, roleList.getMetainfo().getTotalCount());
		assertEquals(2, roleList.getData().size());

		Iterator<RoleResponse> roleIt = roleList.getData().iterator();
		RoleResponse roleB = roleIt.next();
		RoleResponse roleA = roleIt.next();
		assertEquals(role().getUuid(), roleA.getUuid());
		assertEquals(roleUuid, roleB.getUuid());
	}

	@Test
	public void testAddRoleToGroup() throws Exception {
		String roleUuid;
		String groupUuid;
		try (Trx tx = new Trx(db)) {
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", null, user());
			roleUuid = extraRole.getUuid();
			role().grantPermissions(extraRole, READ_PERM);
			assertEquals(1, group().getRoles().size());
			groupUuid = group().getUuid();
			tx.success();
		}

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
		try (Trx tx = new Trx(db)) {
			assertEquals(1, group().getRoles().size());
			uuid = group().getUuid();
		}

		Future<GroupResponse> future = getClient().addRoleToGroup(uuid, "bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testAddNoPermissionRoleToGroup() throws Exception {
		String roleUuid;
		String groupUuid;
		try (Trx tx = new Trx(db)) {
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", null, user());
			roleUuid = extraRole.getUuid();
			assertEquals(1, group().getRoles().size());
			groupUuid = group().getUuid();
			tx.success();
		}

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
		try (Trx tx = new Trx(db)) {
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", null, user());
			roleUuid = extraRole.getUuid();
			group().addRole(extraRole);
			role().grantPermissions(extraRole, READ_PERM);
			tx.success();
			assertEquals(2, group().getRoles().size());
			groupUuid = group().getUuid();
		}

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
		try (Trx tx = new Trx(db)) {
			Group group = group();
			RoleRoot root = meshRoot().getRoleRoot();

			extraRole = root.create("extraRole", null, user());
			role().grantPermissions(extraRole, READ_PERM);
			tx.success();
		}
		try (Trx tx = new Trx(db)) {
			Future<GroupResponse> future = getClient().addRoleToGroup(group().getUuid(), extraRole.getUuid());
			latchFor(future);
			assertSuccess(future);
			GroupResponse restGroup = future.result();
			test.assertGroup(group(), restGroup);
		}

		try (Trx tx = new Trx(db)) {
			assertTrue("Role should be assigned to group.", group().hasRole(extraRole));
		}
	}

	@Test
	public void testAddRoleToGroupWithoutPermOnGroup() throws Exception {
		Role extraRole;
		try (Trx tx = new Trx(db)) {
			Group group = group();
			RoleRoot root = meshRoot().getRoleRoot();
			extraRole = root.create("extraRole", null, user());
			role().revokePermissions(group, UPDATE_PERM);
			tx.success();
		}
		try (Trx tx = new Trx(db)) {
			Future<GroupResponse> future = getClient().addRoleToGroup(group().getUuid(), extraRole.getUuid());
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", group().getUuid());
		}
		try (Trx tx = new Trx(db)) {
			assertFalse("Role should not be assigned to group.", group().hasRole(extraRole));
		}
	}

	@Test
	public void testAddRoleToGroupWithBogusRoleUUID() throws Exception {

		try (Trx tx = new Trx(db)) {
			Future<GroupResponse> future = getClient().addRoleToGroup(group().getUuid(), "bogus");
			latchFor(future);
			expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
		}
	}

	// Group Role Testcases - DELETE / Remove

	@Test
	public void testRemoveRoleFromGroupWithPerm() throws Exception {
		Role extraRole;
		try (Trx tx = new Trx(db)) {
			RoleRoot root = meshRoot().getRoleRoot();
			Group group = group();
			extraRole = root.create("extraRole", group, user());

			assertNotNull(group.getUuid());
			assertNotNull(extraRole.getUuid());

			role().grantPermissions(extraRole, READ_PERM);
			role().grantPermissions(group, UPDATE_PERM);
			tx.success();
		}

		Future<GroupResponse> future;
		try (Trx tx = new Trx(db)) {
			future = getClient().removeRoleFromGroup(group().getUuid(), extraRole.getUuid());
			latchFor(future);
			assertSuccess(future);
		}

		try (Trx tx = new Trx(db)) {
			GroupResponse restGroup = future.result();
			test.assertGroup(group(), restGroup);
			assertFalse("Role should now no longer be assigned to group.", group().hasRole(extraRole));
		}
	}

	@Test
	public void testRemoveRoleFromGroupWithoutPerm() throws Exception {
		Role extraRole;
		try (Trx tx = new Trx(db)) {
			Group group = group();
			RoleRoot root = meshRoot().getRoleRoot();
			extraRole = root.create("extraRole", group, user());
			role().revokePermissions(group, UPDATE_PERM);
			tx.success();
		}

		try (Trx tx = new Trx(db)) {
			Future<GroupResponse> future = getClient().removeRoleFromGroup(group().getUuid(), extraRole.getUuid());
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", group().getUuid());
		}

		try (Trx tx = new Trx(db)) {
			assertTrue("Role should be stil assigned to group.", group().hasRole(extraRole));
		}
	}
}
