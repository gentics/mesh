package com.gentics.mesh.core.role;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingGroupDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.google.common.collect.Iterators;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(testSize = PROJECT, startServer = false)
public class RoleTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			RoleReference reference = role().transformToReference();
			assertNotNull(reference);
			assertEquals(role().getUuid(), reference.getUuid());
			assertEquals(role().getName(), reference.getName());
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		try (Tx tx = tx()) {
			String roleName = "test";
			RoleDao roleDao = tx.roleDao();
			HibRole createdRole = roleDao.create(roleName, user());
			assertNotNull(createdRole);
			String uuid = createdRole.getUuid();
			HibRole role = tx.roleDao().findByUuid(uuid);
			assertNotNull(role);
			assertEquals(roleName, role.getName());
		}
	}

	@Test
	public void testGrantPermission() {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			RoleDao roleDao = tx.roleDao();
			HibRole role = role();
			HibNode node = folder("news");
			roleDao.grantPermissions(role, node, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);

			// node2
			HibNode parentNode = folder("2015");
			HibNode node2 = nodeDao.create(parentNode, user(), getSchemaContainer().getLatestVersion(), project());
			// NodeFieldContainer englishContainer = node2.getFieldContainer(english());
			// englishContainer.setI18nProperty("content", "Test");
			roleDao.grantPermissions(role, node2, READ_PERM, DELETE_PERM);
			roleDao.grantPermissions(role, node2, CREATE_PERM);
			Set<InternalPermission> permissions = roleDao.getPermissions(role, node2);

			assertNotNull(permissions);
			assertTrue(permissions.contains(CREATE_PERM));
			assertTrue(permissions.contains(READ_PERM));
			assertTrue(permissions.contains(DELETE_PERM));
			assertFalse(permissions.contains(UPDATE_PERM));
			roleDao.grantPermissions(role, role, CREATE_PERM);
		}
	}

	@Test
	public void testIsPermitted() throws Exception {
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			HibUser user = user();
			int nRuns = 2000;
			for (int i = 0; i < nRuns; i++) {
				userDao.hasPermission(user, folder("news"), READ_PERM);
			}
		}
	}

	@Test
	public void testGrantPermissionTwice() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole role = role();
			HibNode node = folder("news");

			roleDao.grantPermissions(role, node, CREATE_PERM);
			roleDao.grantPermissions(role, node, CREATE_PERM);

			Set<InternalPermission> permissions = roleDao.getPermissions(role, node);
			assertNotNull(permissions);
			assertTrue(permissions.contains(CREATE_PERM));
			assertTrue(permissions.contains(READ_PERM));
			assertTrue(permissions.contains(DELETE_PERM));
			assertTrue(permissions.contains(UPDATE_PERM));
		}
	}

	@Test
	public void testGrantMultiplePermission() {
		// change permissions on multiple roles
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole admin = roleDao.findByName("admin");
			assertThat(admin).as("Admin role").isNotNull();
			HibRole anonymous = roleDao.findByName("anonymous");
			assertThat(anonymous).as("Anonymous role").isNotNull();
			HibRole testRole = role();
			assertThat(testRole).as("Test role").isNotNull();
			HibNode node = folder("news");

			assertThat(roleDao.grantPermissions(new HashSet<>(Arrays.asList(admin, anonymous)), node, false,
					CREATE_PERM, READ_PERM)).as("Changed permissions").isTrue();

			assertThat(roleDao.getPermissions(admin, node)).as("Permissions for admin").containsOnly(CREATE_PERM,
					READ_PERM);
			assertThat(roleDao.getPermissions(anonymous, node)).as("Permissions for anonymous")
					.containsOnly(CREATE_PERM, READ_PERM);
			assertThat(roleDao.getPermissions(testRole, node)).as("Permissions for test role").containsOnly(CREATE_PERM,
					PUBLISH_PERM, UPDATE_PERM, READ_PERM, DELETE_PERM, READ_PUBLISHED_PERM);
			tx.success();
		}

		// change permissions and check the unmentioned roles are not touched
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole admin = roleDao.findByName("admin");
			assertThat(admin).as("Admin role").isNotNull();
			HibRole anonymous = roleDao.findByName("anonymous");
			assertThat(anonymous).as("Anonymous role").isNotNull();
			HibRole testRole = role();
			assertThat(testRole).as("Test role").isNotNull();
			HibNode node = folder("news");

			assertThat(roleDao.grantPermissions(new HashSet<>(Arrays.asList(admin, testRole)), node, false,
					UPDATE_PERM)).as("Changed permissions").isTrue();

			assertThat(roleDao.getPermissions(admin, node)).as("Permissions for admin").containsOnly(CREATE_PERM,
					READ_PERM, UPDATE_PERM);
			assertThat(roleDao.getPermissions(anonymous, node)).as("Permissions for anonymous")
					.containsOnly(CREATE_PERM, READ_PERM);
			assertThat(roleDao.getPermissions(testRole, node)).as("Permissions for test role").containsOnly(CREATE_PERM,
					PUBLISH_PERM, UPDATE_PERM, READ_PERM, DELETE_PERM, READ_PUBLISHED_PERM);
			tx.success();
		}

		// "change" something that is already set
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole admin = roleDao.findByName("admin");
			assertThat(admin).as("Admin role").isNotNull();
			HibRole anonymous = roleDao.findByName("anonymous");
			assertThat(anonymous).as("Anonymous role").isNotNull();
			HibRole testRole = role();
			assertThat(testRole).as("Test role").isNotNull();
			HibNode node = folder("news");

			assertThat(roleDao.grantPermissions(new HashSet<>(Arrays.asList(admin, anonymous, testRole)), node, false,
					CREATE_PERM, READ_PERM)).as("Changed permissions").isFalse();
			tx.success();
		}
	}

	@Test
	public void testGrantMultiplePermissionExclusive() {
		// change permissions on multiple roles
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole admin = roleDao.findByName("admin");
			assertThat(admin).as("Admin role").isNotNull();
			HibRole anonymous = roleDao.findByName("anonymous");
			assertThat(anonymous).as("Anonymous role").isNotNull();
			HibRole testRole = role();
			assertThat(testRole).as("Test role").isNotNull();
			HibNode node = folder("news");

			assertThat(roleDao.grantPermissions(new HashSet<>(Arrays.asList(admin, anonymous)), node, true,
					CREATE_PERM, READ_PERM)).as("Changed permissions").isTrue();

			assertThat(roleDao.getPermissions(admin, node)).as("Permissions for admin").containsOnly(CREATE_PERM,
					READ_PERM);
			assertThat(roleDao.getPermissions(anonymous, node)).as("Permissions for anonymous")
					.containsOnly(CREATE_PERM, READ_PERM);
			assertThat(roleDao.getPermissions(testRole, node)).as("Permissions for test role")
					.containsOnly(PUBLISH_PERM, UPDATE_PERM, DELETE_PERM, READ_PUBLISHED_PERM);
			tx.success();
		}

		// "change" something that is already set
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole admin = roleDao.findByName("admin");
			assertThat(admin).as("Admin role").isNotNull();
			HibRole anonymous = roleDao.findByName("anonymous");
			assertThat(anonymous).as("Anonymous role").isNotNull();
			HibRole testRole = role();
			assertThat(testRole).as("Test role").isNotNull();
			HibNode node = folder("news");

			assertThat(roleDao.grantPermissions(new HashSet<>(Arrays.asList(admin, anonymous)), node, true, READ_PERM))
					.as("Changed permissions").isFalse();
			tx.success();
		}

		// do a change, that only restricts other roles further
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole admin = roleDao.findByName("admin");
			assertThat(admin).as("Admin role").isNotNull();
			HibRole anonymous = roleDao.findByName("anonymous");
			assertThat(anonymous).as("Anonymous role").isNotNull();
			HibRole testRole = role();
			assertThat(testRole).as("Test role").isNotNull();
			HibNode node = folder("news");

			assertThat(roleDao.grantPermissions(new HashSet<>(Arrays.asList(admin)), node, true, READ_PERM))
					.as("Changed permissions").isTrue();

			assertThat(roleDao.getPermissions(admin, node)).as("Permissions for admin").containsOnly(CREATE_PERM,
					READ_PERM);
			assertThat(roleDao.getPermissions(anonymous, node)).as("Permissions for anonymous")
					.containsOnly(CREATE_PERM);
			assertThat(roleDao.getPermissions(testRole, node)).as("Permissions for test role")
					.containsOnly(PUBLISH_PERM, UPDATE_PERM, DELETE_PERM, READ_PUBLISHED_PERM);
			tx.success();
		}

	}

	@Test
	public void testGetPermissions() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole role = role();
			HibNode node = folder("news");
			assertEquals(6, roleDao.getPermissions(role, node).size());
		}
	}

	@Test
	public void testGetMultiplePermissions() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole admin = roleDao.findByName("admin");
			assertThat(admin).as("Admin role").isNotNull();
			HibRole anonymous = roleDao.findByName("anonymous");
			assertThat(anonymous).as("Anonymous role").isNotNull();
			HibRole testRole = role();
			assertThat(testRole).as("Test role").isNotNull();
			HibNode node = folder("news");
			Map<HibRole, Set<InternalPermission>> result = roleDao.getPermissions(new HashSet<>(Arrays.asList(admin, anonymous, testRole)), node);
			assertThat(result).as("Permissions result")
				.containsOnlyKeys(admin, anonymous, testRole)
				.containsEntry(testRole, new HashSet<>(Arrays.asList(CREATE_PERM, PUBLISH_PERM, UPDATE_PERM, READ_PERM, DELETE_PERM, READ_PUBLISHED_PERM)))
				.containsEntry(admin, Collections.emptySet())
				.containsEntry(anonymous, Collections.emptySet());
		}
	}

	@Test
	public void testGetMultiplePermissionsForNoRoles() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibNode node = folder("news");
			Map<HibRole, Set<InternalPermission>> result = roleDao.getPermissions(Collections.emptySet(), node);
			assertThat(result).as("Permissions result")
				.isEmpty();
		}
	}

	@Test
	public void testRevokePermission() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole role = role();
			HibNode node = folder("news");
			roleDao.revokePermissions(role, node, CREATE_PERM);

			Set<InternalPermission> permissions = roleDao.getPermissions(role, node);
			assertNotNull(permissions);
			assertFalse(permissions.contains(CREATE_PERM));
			assertTrue(permissions.contains(DELETE_PERM));
			assertTrue(permissions.contains(UPDATE_PERM));
			assertTrue(permissions.contains(READ_PERM));
		}
	}

	@Test
	public void testRevokeMultiplePermissions() {
		// revoke permissions
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole admin = roleDao.findByName("admin");
			assertThat(admin).as("Admin role").isNotNull();
			HibRole anonymous = roleDao.findByName("anonymous");
			assertThat(anonymous).as("Anonymous role").isNotNull();
			HibRole testRole = role();
			assertThat(testRole).as("Test role").isNotNull();
			HibNode node = folder("news");

			roleDao.grantPermissions(admin, node, CREATE_PERM, UPDATE_PERM);
			roleDao.grantPermissions(anonymous, node, CREATE_PERM);

			assertThat(roleDao.revokePermissions(new HashSet<>(Arrays.asList(admin, testRole)), node, CREATE_PERM,
					READ_PERM)).as("Permissions were changed").isTrue();

			assertThat(roleDao.getPermissions(admin, node)).as("Permissions for admin").containsOnly(UPDATE_PERM);
			assertThat(roleDao.getPermissions(anonymous, node)).as("Permissions for anonymous")
					.containsOnly(CREATE_PERM);
			assertThat(roleDao.getPermissions(testRole, node)).as("Permissions for test role")
					.containsOnly(PUBLISH_PERM, UPDATE_PERM, DELETE_PERM, READ_PUBLISHED_PERM);

			tx.success();
		}

		// try again (nothing is changed)
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibRole admin = roleDao.findByName("admin");
			assertThat(admin).as("Admin role").isNotNull();
			HibRole anonymous = roleDao.findByName("anonymous");
			assertThat(anonymous).as("Anonymous role").isNotNull();
			HibRole testRole = role();
			assertThat(testRole).as("Test role").isNotNull();
			HibNode node = folder("news");

			assertThat(roleDao.revokePermissions(new HashSet<>(Arrays.asList(admin, testRole)), node, CREATE_PERM,
					READ_PERM)).as("Permissions were changed").isFalse();

			tx.success();
		}
	}

	@Test
	public void testRevokePermissionOnGroupRoot() throws Exception {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			roleDao.revokePermissions(role(), tx.data().permissionRoots().group(), CREATE_PERM);
			HibUser user = user();
			assertFalse("The create permission to the groups root node should have been revoked.", userDao.hasPermission(user, tx.data().permissionRoots().group(), CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			final String roleName = "test2";
			RoleDao roleDao = tx.roleDao();
			long nRolesBefore = roleDao.count();
			HibRole role = roleDao.create(roleName, user());
			assertNotNull(role);
			long nRolesAfter = roleDao.count();
			assertEquals(nRolesBefore + 1, nRolesAfter);
		}
	}

	@Test
	public void testRoleAddCrudPermissions() {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			HibUser requestUser = user();
			// userRoot.findMeshAuthUserByUsername(requestUser.getUsername())
			HibNode parentNode = folder("news");
			assertNotNull(parentNode);

			// Grant all permissions to all roles
			for (HibRole role : roles().values()) {
				for (InternalPermission perm : InternalPermission.values()) {
					roleDao.grantPermissions(role, parentNode, perm);
				}
			}

			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			HibNode node = nodeDao.create(parentNode, user(), getSchemaContainer().getLatestVersion(), project());
			assertEquals(0, userDao.getPermissions(requestUser, node).size());
			userDao.inheritRolePermissions(requestUser, parentNode, node);
			ac.data().clear();
			assertEquals(6, userDao.getPermissions(requestUser, node).size());

			try (Tx tx2 = tx()) {
				for (HibRole role : roles().values()) {
					for (InternalPermission permission : InternalPermission.values()) {
						assertTrue("The role {" + role.getName() + "} does not grant perm {" + permission.getRestPerm().getName() + "} to the node {"
							+ node.getUuid() + "} but it should since the parent object got this role permission.",
							roleDao.hasPermission(role, permission,
								node));
					}
				}
			}
		}
	}

	@Test
	public void testRolesOfGroup() throws InvalidArgumentException {

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			PersistingGroupDao groupDao = tx.<CommonTx>unwrap().groupDao();

			HibRole extraRole = roleDao.create("extraRole", user());
			groupDao.addRole(group(), extraRole);

			// Multiple add role calls should not affect the result
			groupDao.addRole(group(), extraRole);
			groupDao.addRole(group(), extraRole);
			groupDao.addRole(group(), extraRole);
			groupDao.addRole(group(), extraRole);
			groupDao.mergeIntoPersisted(group());

			roleDao.grantPermissions(role(), extraRole, READ_PERM);
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			HibUser requestUser = ac.getUser();
			Page<? extends HibRole> roles = groupDao.getRoles(group(), requestUser, new PagingParametersImpl(1, 10L));
			assertEquals(2, roles.getSize());
			
			assertEquals(1, roleDao.getGroups(extraRole).count());

			// assertEquals(2, roles.getTotalElements());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		int roleCount = roles().size();
		try (Tx tx = tx()) {
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			Page<? extends HibRole> page = tx.roleDao().findAll(ac, new PagingParametersImpl(1, 5L));
			assertEquals(roleCount, page.getTotalElements());
			assertEquals(roleCount, page.getSize());

			page = tx.roleDao().findAll(ac, new PagingParametersImpl(1, 15L));
			assertEquals(roleCount, page.getTotalElements());
			assertEquals(roleCount, page.getSize());
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Tx tx = tx()) {
			assertNotNull(tx.roleDao().findByName(role().getName()));
			assertNull(tx.roleDao().findByName("bogus"));
		}
	}

	@Test
	@Override
	public void testFindByUUID() {
		try (Tx tx = tx()) {
			HibRole role = tx.roleDao().findByUuid(role().getUuid());
			assertNotNull(role);
			role = tx.roleDao().findByUuid("bogus");
			assertNull(role);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			
			HibRole role = role();
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			RoleResponse restModel = roleDao.transformToRestSync(role, ac, 0);

			assertNotNull(restModel);
			assertEquals(role.getName(), restModel.getName());
			assertEquals(role.getUuid(), restModel.getUuid());
		}

	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		String roleName = "test";
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();

			HibRole role = roleDao.create(roleName, user());
			String uuid = role.getUuid();
			role = roleDao.findByUuid(uuid);
			assertNotNull(role);
			roleDao.delete(role);
			HibRole foundRole = roleDao.findByUuid(uuid);
			assertNull(foundRole);
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			RoleDao roleDao = tx.roleDao();

			InternalActionContext ac = mockActionContext();
			HibRole role = roleDao.create("SuperUser", user());
			assertFalse(userDao.hasPermission(user(), role, InternalPermission.CREATE_PERM));
			userDao.inheritRolePermissions(user(), tx.data().permissionRoots().user(), role);
			ac.data().clear();
			assertTrue(userDao.hasPermission(user(), role, InternalPermission.CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			long size = Iterators.size(tx.roleDao().findAll().iterator());
			assertEquals(roles().size(), size);
		}
	}

	@Test
	@Override
	public void testRead() {
		try (Tx tx = tx()) {
			HibRole role = role();
			assertEquals("joe1_role", role.getName());
			assertNotNull(role.getUuid());

			assertNotNull(role.getCreationTimestamp());
			assertNotNull(role.getCreator());

			assertNotNull(role.getEditor());
			assertNotNull(role.getLastEditedTimestamp());
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		try (Tx tx = tx()) {
			String uuid;
			BulkActionContext context = createBulkContext();
			try (Tx tx2 = tx()) {
				RoleDao roleDao = tx2.roleDao();
				tx2.<CommonTx>unwrap().data().setBulkActionContext(context);
				HibRole role = tx.roleDao().findByUuid(role().getUuid());
				uuid = role.getUuid();
				roleDao.delete(role);
				tx2.success();
			}
			assertNull(tx.roleDao().findByUuid(uuid));
			assertEquals("The role event was not included in the batch", 1, context.batch().getEntries().size());
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (Tx tx = tx()) {
			HibRole role = role();
			role.setName("newName");
			assertEquals("newName", role.getName());
			// assertEquals(1,role.getProjects());
			// TODO test project assignments
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.READ_PERM, role());
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.DELETE_PERM, role());
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.UPDATE_PERM, role());
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.CREATE_PERM, role());
		}
	}
}
