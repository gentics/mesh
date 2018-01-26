package com.gentics.mesh.core.group;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = true)
public class GroupRolesEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadRolesByGroup() throws Exception {
		String roleUuid;
		try (Tx tx = tx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", user());
			group().addRole(extraRole);

			roleUuid = extraRole.getUuid();
			role().grantPermissions(extraRole, READ_PERM);
			tx.success();
		}

		RoleListResponse roleList = call(() -> client().findRolesForGroup(groupUuid()));
		assertEquals(2, roleList.getMetainfo().getTotalCount());
		assertEquals(2, roleList.getData().size());

		Set<String> listedRoleUuids = new HashSet<>();
		for (RoleResponse role : roleList.getData()) {
			listedRoleUuids.add(role.getUuid());
		}

		try (Tx tx = tx()) {
			assertTrue(listedRoleUuids.contains(role().getUuid()));
			assertTrue(listedRoleUuids.contains(roleUuid));
		}
	}

	@Test
	public void testAddRoleToGroup() throws Exception {
		String roleUuid;
		try (Tx tx = tx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", user());
			roleUuid = extraRole.getUuid();
			role().grantPermissions(extraRole, READ_PERM);
			assertEquals(1, group().getRoles().size());
			tx.success();
		}

		searchProvider().clear().blockingAwait();
		GroupResponse restGroup = call(() -> client().addRoleToGroup(groupUuid(), roleUuid));
		assertThat(dummySearchProvider()).hasStore(Group.composeIndexName(), groupUuid());
		// The role is not updated since it is not changing
		assertThat(dummySearchProvider()).hasEvents(1, 0, 0, 0);
		// Check for idempotency
		call(() -> client().addRoleToGroup(groupUuid(), roleUuid));
		assertThat(dummySearchProvider()).hasEvents(1, 0, 0, 0);

		try (Tx tx = tx()) {
			assertEquals(1, restGroup.getRoles().stream().filter(ref -> ref.getName().equals("extraRole")).count());
			assertEquals(2, group().getRoles().size());
		}

	}

	@Test
	public void testAddBogusRoleToGroup() throws Exception {
		try (Tx tx = tx()) {
			assertEquals(1, group().getRoles().size());
		}
		call(() -> client().addRoleToGroup(groupUuid(), "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testAddNoPermissionRoleToGroup() throws Exception {
		String roleUuid;
		try (Tx tx = tx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", user());
			roleUuid = extraRole.getUuid();
			assertEquals(1, group().getRoles().size());
			tx.success();
		}

		call(() -> client().addRoleToGroup(groupUuid(), roleUuid), FORBIDDEN, "error_missing_perm", roleUuid);

		try (Tx tx = tx()) {
			assertEquals(1, group().getRoles().size());
		}
	}

	@Test
	public void testRemoveRoleFromGroup() throws Exception {
		String roleUuid;
		try (Tx tx = tx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", user());
			roleUuid = extraRole.getUuid();
			group().addRole(extraRole);
			role().grantPermissions(extraRole, READ_PERM);
			tx.success();
			assertEquals(2, group().getRoles().size());
			searchProvider().clear().blockingAwait();
		}

		call(() -> client().removeRoleFromGroup(groupUuid(), roleUuid));
		assertThat(dummySearchProvider()).hasStore(Group.composeIndexName(), groupUuid());
		// The role is not updated since it is not changing
		assertThat(dummySearchProvider()).hasEvents(1, 0, 0, 0);

		GroupResponse restGroup = call(() -> client().findGroupByUuid(groupUuid()));
		assertFalse(restGroup.getRoles().contains("extraRole"));

		try (Tx tx = tx()) {
			assertEquals(1, group().getRoles().size());
		}

	}

	@Test
	public void testAddRoleToGroupWithPerm() throws Exception {
		Role extraRole;
		try (Tx tx = tx()) {
			RoleRoot root = meshRoot().getRoleRoot();

			extraRole = root.create("extraRole", user());
			role().grantPermissions(extraRole, READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			GroupResponse restGroup = call(() -> client().addRoleToGroup(group().getUuid(), extraRole.getUuid()));
			assertThat(restGroup).matches(group());
		}

		try (Tx tx = tx()) {
			assertTrue("Role should be assigned to group.", group().hasRole(extraRole));
		}
	}

	@Test
	public void testAddRoleToGroupWithoutPermOnGroup() throws Exception {
		Role extraRole;
		try (Tx tx = tx()) {
			Group group = group();
			RoleRoot root = meshRoot().getRoleRoot();
			extraRole = root.create("extraRole", user());
			role().revokePermissions(group, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().addRoleToGroup(groupUuid(), extraRole.getUuid()), FORBIDDEN, "error_missing_perm", groupUuid());
		}

		try (Tx tx = tx()) {
			assertFalse("Role should not be assigned to group.", group().hasRole(extraRole));
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
		Role extraRole;
		try (Tx tx = tx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			Group group = group();
			extraRole = root.create("extraRole", user());
			group.addRole(extraRole);

			assertNotNull(group.getUuid());
			assertNotNull(extraRole.getUuid());

			role().grantPermissions(extraRole, READ_PERM);
			role().grantPermissions(group, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().removeRoleFromGroup(groupUuid(), extraRole.getUuid()));
		}

		try (Tx tx = tx()) {
			GroupResponse restGroup = call(() -> client().findGroupByUuid(groupUuid()));
			assertThat(restGroup).matches(group());
			assertFalse("Role should now no longer be assigned to group.", group().hasRole(extraRole));
		}
	}

	@Test
	public void testRemoveRoleFromGroupWithoutPerm() throws Exception {
		String extraRoleUuid;
		Role extraRole;
		try (Tx tx = tx()) {
			Group group = group();
			RoleRoot root = meshRoot().getRoleRoot();
			extraRole = root.create("extraRole", user());
			extraRoleUuid = extraRole.getUuid();
			group.addRole(extraRole);
			role().revokePermissions(group, UPDATE_PERM);
			tx.success();
		}

		call(() -> client().removeRoleFromGroup(groupUuid(), extraRoleUuid), FORBIDDEN, "error_missing_perm", groupUuid());

		try (Tx tx = tx()) {
			assertTrue("Role should be stil assigned to group.", group().hasRole(extraRole));
		}
	}
}
