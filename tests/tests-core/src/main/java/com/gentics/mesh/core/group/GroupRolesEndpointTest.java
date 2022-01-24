package com.gentics.mesh.core.group;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_ROLE_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_ROLE_UNASSIGNED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.PersistingGroupDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(elasticsearch = TRACKING, testSize = PROJECT, startServer = true)
public class GroupRolesEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadRolesByGroup() throws Exception {
		String roleUuid;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			PersistingGroupDao groupDao = tx.<CommonTx>unwrap().groupDao();

			HibRole extraRole = roleDao.create("extraRole", user());
			HibGroup group = groupDao.findByUuid(group().getUuid());
			groupDao.addRole(group, extraRole);
			groupDao.mergeIntoPersisted(group);

			roleUuid = extraRole.getUuid();
			roleDao.grantPermissions(role(), extraRole, READ_PERM);
			tx.success();
		}

		RoleListResponse roleList = call(() -> client().findRolesForGroup(groupUuid(), new PagingParametersImpl()));
		assertThat(roleList.getMetainfo().getTotalCount()).as("Total count").isEqualTo(2);

		RoleResponse expectedTestRole = new RoleResponse();
		expectedTestRole.setUuid(role().getUuid());
		expectedTestRole.setName(role().getName());

		RoleResponse expectedExtraRole = new RoleResponse();
		expectedExtraRole.setUuid(roleUuid);
		expectedExtraRole.setName("extraRole");

		assertThat(roleList.getData()).as("Roles of group").usingElementComparatorOnFields("uuid", "name")
				.containsOnly(expectedTestRole, expectedExtraRole);

		// revoke read permission on the extra role
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole extraRole = roleDao.findByUuid(roleUuid);
			roleDao.revokePermissions(role(), extraRole, READ_PERM);
			tx.success();
		}

		roleList = call(() -> client().findRolesForGroup(groupUuid(), new PagingParametersImpl()));
		assertThat(roleList.getMetainfo().getTotalCount()).as("Total count").isEqualTo(1);
		assertThat(roleList.getData()).as("Roles of group").usingElementComparatorOnFields("uuid", "name")
			.containsOnly(expectedTestRole);
	}

	@Test
	public void testAddRoleToGroup() throws Exception {
		String roleName = "extraRole";
		String groupName = tx(() -> group().getName());
		String groupUuid = groupUuid();
		String roleUuid = tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			GroupDao groupDao = tx.groupDao();

			HibRole extraRole = roleDao.create(roleName, user());
			roleDao.grantPermissions(role(), extraRole, READ_PERM);
			assertEquals(1, groupDao.getRoles(group()).count());
			return extraRole.getUuid();
		});

		searchProvider().clear().blockingAwait();
		expect(GROUP_ROLE_ASSIGNED).match(1, GroupRoleAssignModel.class, event -> {
			GroupReference group = event.getGroup();
			assertNotNull(group);
			assertEquals("The group name was not set.", groupName, group.getName());
			assertEquals("The group uuid was not set.", groupUuid, group.getUuid());

			RoleReference role = event.getRole();
			assertNotNull(role);
			assertEquals("The role name was not set.", roleName, role.getName());
			assertEquals("The role uuid was not set.", roleUuid, role.getUuid());
		}).one();

		GroupResponse restGroup = call(() -> client().addRoleToGroup(groupUuid(), roleUuid));
		awaitEvents();
		waitForSearchIdleEvent();

		// The role and group is not updated since it is not changing
		assertThat(trackingSearchProvider()).hasEvents(0, 0, 0, 0, 0);

		// Check for idempotency
		expect(GROUP_ROLE_ASSIGNED).none();
		call(() -> client().addRoleToGroup(groupUuid(), roleUuid));
		awaitEvents();

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertEquals(1, restGroup.getRoles().stream().filter(ref -> ref.getName().equals("extraRole")).count());
			assertEquals(2, groupDao.getRoles(group()).count());
		}

	}

	@Test
	public void testAddBogusRoleToGroup() throws Exception {
		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertEquals(1, groupDao.getRoles(group()).count());
		}
		call(() -> client().addRoleToGroup(groupUuid(), "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testAddNoPermissionRoleToGroup() throws Exception {
		String roleUuid;
		HibRole extraRole;
		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			RoleDao roleDao = tx.roleDao();

			extraRole = roleDao.create("extraRole", user());
			roleUuid = extraRole.getUuid();
			assertEquals(1, groupDao.getRoles(group()).count());
			tx.success();
		}

		expect(GROUP_ROLE_ASSIGNED).none();
		call(() -> client().addRoleToGroup(groupUuid(), roleUuid), FORBIDDEN, "error_missing_perm", roleUuid, READ_PERM.getRestPerm().getName());
		awaitEvents();

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertEquals(1, groupDao.getRoles(group()).count());
		}

		// Now confirm that the request works once we set the perm
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.grantPermissions(role(), extraRole, READ_PERM);
			tx.success();
		}

		expect(GROUP_ROLE_ASSIGNED).one();
		call(() -> client().addRoleToGroup(groupUuid(), roleUuid));
		awaitEvents();

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertEquals(2, groupDao.getRoles(group()).count());
		}

	}

	@Test
	public void testRemoveRoleFromGroup() throws Exception {
		String groupName = tx(() -> group().getName());
		String groupUuid = groupUuid();
		String roleName = "extraRole";
		String roleUuid = tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			PersistingGroupDao groupDao = tx.<CommonTx>unwrap().groupDao();

			HibRole extraRole = roleDao.create(roleName, user());
			HibGroup group = groupDao.findByUuid(group().getUuid());
			groupDao.addRole(group, extraRole);
			groupDao.mergeIntoPersisted(group);
			roleDao.grantPermissions(role(), extraRole, READ_PERM);
			assertEquals(2, groupDao.getRoles(group()).count());
			searchProvider().reset();
			return extraRole.getUuid();
		});

		expect(GROUP_ROLE_UNASSIGNED).match(1, GroupRoleAssignModel.class, event -> {
			GroupReference group = event.getGroup();
			assertNotNull(group);
			assertEquals("The group name was not set.", groupName, group.getName());
			assertEquals("The group uuid was not set.", groupUuid, group.getUuid());

			RoleReference role = event.getRole();
			assertNotNull(role);
			assertEquals("The role name was not set.", roleName, role.getName());
			assertEquals("The role uuid was not set.", roleUuid, role.getUuid());
		}).total(1);

		call(() -> client().removeRoleFromGroup(groupUuid(), roleUuid));
		awaitEvents();
		// The role and group is not updated since it is not changing
		assertThat(trackingSearchProvider()).hasEvents(0, 0, 0, 0, 0);

		GroupResponse restGroup = call(() -> client().findGroupByUuid(groupUuid()));
		assertFalse(restGroup.getRoles().stream()
			.map(RoleReference::getName)
			.anyMatch("extraRole"::equals));

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertEquals(1, groupDao.getRoles(group()).count());
		}

		// Test for idempotency
		expect(GROUP_ROLE_UNASSIGNED).none();
		call(() -> client().removeRoleFromGroup(groupUuid(), roleUuid));
		awaitEvents();

	}

	@Test
	public void testAddRoleToGroupWithPerm() throws Exception {
		HibRole extraRole;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();

			extraRole = roleDao.create("extraRole", user());
			roleDao.grantPermissions(role(), extraRole, READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			GroupResponse restGroup = call(() -> client().addRoleToGroup(group().getUuid(), extraRole.getUuid()));
			assertThat(restGroup).matches(group());
		}

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			HibGroup group = groupDao.findByUuid(group().getUuid());
			assertTrue("Role should be assigned to group.", groupDao.hasRole(group, tx.roleDao().findByUuid(extraRole.getUuid())));
		}
	}

	@Test
	public void testAddRoleToGroupWithoutPermOnGroup() throws Exception {
		HibRole extraRole;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();

			HibGroup group = group();
			extraRole = roleDao.create("extraRole", user());
			roleDao.revokePermissions(role(), group, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().addRoleToGroup(groupUuid(), extraRole.getUuid()), FORBIDDEN, "error_missing_perm", groupUuid(),
				UPDATE_PERM.getRestPerm().getName());
		}

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertFalse("Role should not be assigned to group.", groupDao.hasRole(group(), extraRole));
		}
	}

	@Test
	public void testAddRoleToGroupWithBogusRoleUUID() throws Exception {
		try (Tx tx = tx()) {
			call(() -> client().addRoleToGroup(group().getUuid(), "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
		}
	}

	@Test
	public void testRemoveRoleFromGroupWithPerm() throws Exception {
		HibRole extraRole;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			PersistingGroupDao groupDao = tx.<CommonTx>unwrap().groupDao();

			HibGroup group = group();
			extraRole = roleDao.create("extraRole", user());
			groupDao.addRole(group, extraRole);
			groupDao.mergeIntoPersisted(group);

			assertNotNull(group.getUuid());
			assertNotNull(extraRole.getUuid());

			roleDao.grantPermissions(role(), extraRole, READ_PERM);
			roleDao.grantPermissions(role(), group, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			HibGroup group = groupDao.findByUuid(group().getUuid());
			GroupResponse restGroup = call(() -> client().findGroupByUuid(groupUuid()));
			assertThat(restGroup).matches(group());
			assertTrue("Role should now be assigned to group.", groupDao.hasRole(group, tx.roleDao().findByUuid(extraRole.getUuid())));
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().removeRoleFromGroup(groupUuid(), extraRole.getUuid()));
		}

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			HibGroup group = groupDao.findByUuid(group().getUuid());
			GroupResponse restGroup = call(() -> client().findGroupByUuid(groupUuid()));
			assertThat(restGroup).matches(group());
			assertFalse("Role should now no longer be assigned to group.", groupDao.hasRole(group, tx.roleDao().findByUuid(extraRole.getUuid())));
			tx.success();
		}
	}

	@Test
	public void testRemoveRoleFromGroupWithoutPerm() throws Exception {
		String extraRoleUuid;
		HibRole extraRole;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			GroupDao groupDao = tx.groupDao();

			HibGroup group = group();
			extraRole = roleDao.create("extraRole", user());
			extraRoleUuid = extraRole.getUuid();
			groupDao.addRole(group, extraRole);
			roleDao.revokePermissions(role(), group, UPDATE_PERM);
			tx.success();
		}

		call(() -> client().removeRoleFromGroup(groupUuid(), extraRoleUuid), FORBIDDEN, "error_missing_perm", groupUuid(),
			UPDATE_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertTrue("Role should be stil assigned to group.", groupDao.hasRole(group(), extraRole));
		}
	}
}
