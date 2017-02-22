package com.gentics.mesh.core.group;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
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
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = true)
public class GroupRolesEndpointTest extends AbstractMeshTest {

	// Group Role Testcases - PUT / Add

	@Test
	public void testReadRolesByGroup() throws Exception {
		try (NoTx noTx = db().noTx()) {

			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", user());
			group().addRole(extraRole);

			String roleUuid = extraRole.getUuid();
			role().grantPermissions(extraRole, READ_PERM);
			String groupUuid = group().getUuid();

			RoleListResponse roleList = call(() -> client().findRolesForGroup(groupUuid));
			assertEquals(2, roleList.getMetainfo().getTotalCount());
			assertEquals(2, roleList.getData().size());

			Set<String> listedRoleUuids = new HashSet<>();
			for (RoleResponse role : roleList.getData()) {
				listedRoleUuids.add(role.getUuid());
			}

			assertTrue(listedRoleUuids.contains(role().getUuid()));
			assertTrue(listedRoleUuids.contains(roleUuid));
		}
	}

	@Test
	public void testAddRoleToGroup() throws Exception {
		try (NoTx noTx = db().noTx()) {

			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", user());
			String roleUuid = extraRole.getUuid();
			role().grantPermissions(extraRole, READ_PERM);
			assertEquals(1, group().getRoles().size());
			String groupUuid = group().getUuid();

			searchProvider().clear();
			GroupResponse restGroup = call(() -> client().addRoleToGroup(groupUuid, roleUuid));
			assertThat(dummySearchProvider()).hasStore(Group.composeIndexName(), Group.composeIndexType(), groupUuid);
			// The role is not updated since it is not changing
			assertThat(dummySearchProvider()).hasEvents(1, 0, 0, 0);

			assertEquals(1, restGroup.getRoles().stream().filter(ref -> ref.getName().equals("extraRole")).count());
			Group group = group();
			assertEquals(2, group.getRoles().size());
		}
	}

	@Test
	public void testAddBogusRoleToGroup() throws Exception {
		try (NoTx noTx = db().noTx()) {
			String uuid;
			assertEquals(1, group().getRoles().size());
			uuid = group().getUuid();

			call(() -> client().addRoleToGroup(uuid, "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
		}
	}

	@Test
	public void testAddNoPermissionRoleToGroup() throws Exception {
		try (NoTx noTx = db().noTx()) {
			String roleUuid;
			String groupUuid;
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", user());
			roleUuid = extraRole.getUuid();
			assertEquals(1, group().getRoles().size());
			groupUuid = group().getUuid();

			call(() -> client().addRoleToGroup(groupUuid, roleUuid), FORBIDDEN, "error_missing_perm", roleUuid);

			Group group = group();
			assertEquals(1, group.getRoles().size());
		}
	}

	@Test
	public void testRemoveRoleFromGroup() throws Exception {
		try (NoTx noTx = db().noTx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", user());
			String roleUuid = extraRole.getUuid();
			group().addRole(extraRole);
			role().grantPermissions(extraRole, READ_PERM);
			assertEquals(2, group().getRoles().size());
			String groupUuid = group().getUuid();

			searchProvider().clear();
			call(() -> client().removeRoleFromGroup(groupUuid, roleUuid));
			assertThat(dummySearchProvider()).hasStore(Group.composeIndexName(), Group.composeIndexType(), groupUuid);
			// The role is not updated since it is not changing
			assertThat(dummySearchProvider()).hasEvents(1, 0, 0, 0);

			GroupResponse restGroup = call(() -> client().findGroupByUuid(groupUuid));
			assertFalse(restGroup.getRoles().contains("extraRole"));

			Group group = group();
			assertEquals(1, group.getRoles().size());
		}
	}

	@Test
	public void testAddRoleToGroupWithPerm() throws Exception {
		try (NoTx noTx = db().noTx()) {
			Role extraRole;
			RoleRoot root = meshRoot().getRoleRoot();

			extraRole = root.create("extraRole", user());
			role().grantPermissions(extraRole, READ_PERM);
			GroupResponse restGroup = call(() -> client().addRoleToGroup(group().getUuid(), extraRole.getUuid()));
			assertThat(restGroup).matches(group());

			assertTrue("Role should be assigned to group.", group().hasRole(extraRole));
		}
	}

	@Test
	public void testAddRoleToGroupWithoutPermOnGroup() throws Exception {
		try (NoTx noTx = db().noTx()) {
			Role extraRole;
			Group group = group();
			RoleRoot root = meshRoot().getRoleRoot();
			extraRole = root.create("extraRole", user());
			role().revokePermissions(group, UPDATE_PERM);
			call(() -> client().addRoleToGroup(group().getUuid(), extraRole.getUuid()), FORBIDDEN, "error_missing_perm", group().getUuid());
			assertFalse("Role should not be assigned to group.", group().hasRole(extraRole));
		}
	}

	@Test
	public void testAddRoleToGroupWithBogusRoleUUID() throws Exception {
		try (NoTx noTx = db().noTx()) {
			call(() -> client().addRoleToGroup(group().getUuid(), "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
		}
	}

	// Group Role Testcases - DELETE / Remove

	@Test
	public void testRemoveRoleFromGroupWithPerm() throws Exception {
		try (NoTx noTx = db().noTx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			Group group = group();
			Role extraRole = root.create("extraRole", user());
			group.addRole(extraRole);

			assertNotNull(group.getUuid());
			assertNotNull(extraRole.getUuid());

			role().grantPermissions(extraRole, READ_PERM);
			role().grantPermissions(group, UPDATE_PERM);

			call(() -> client().removeRoleFromGroup(group().getUuid(), extraRole.getUuid()));

			GroupResponse restGroup = call(() -> client().findGroupByUuid(group.getUuid()));
			assertThat(restGroup).matches(group());
			assertFalse("Role should now no longer be assigned to group.", group().hasRole(extraRole));
		}
	}

	@Test
	public void testRemoveRoleFromGroupWithoutPerm() throws Exception {
		try (NoTx noTx = db().noTx()) {
			Group group = group();
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", user());
			group.addRole(extraRole);
			role().revokePermissions(group, UPDATE_PERM);

			call(() -> client().removeRoleFromGroup(group().getUuid(), extraRole.getUuid()), FORBIDDEN, "error_missing_perm", group().getUuid());
			assertTrue("Role should be stil assigned to group.", group().hasRole(extraRole));
		}
	}
}
