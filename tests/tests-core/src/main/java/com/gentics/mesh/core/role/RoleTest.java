package com.gentics.mesh.core.role;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.user.HibUser;
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
			RoleDaoWrapper roleDao = tx.roleDao();
			HibRole createdRole = roleDao.create(roleName, user());
			assertNotNull(createdRole);
			String uuid = createdRole.getUuid();
			Role role = boot().roleRoot().findByUuid(uuid);
			assertNotNull(role);
			assertEquals(roleName, role.getName());
		}
	}

	@Test
	public void testGrantPermission() {
		try (Tx tx = tx()) {
			NodeDaoWrapper nodeDao = tx.nodeDao();
			RoleDaoWrapper roleDao = tx.roleDao();
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
			UserDaoWrapper userDao = tx.userDao();
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
			RoleDaoWrapper roleDao = tx.roleDao();
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
	public void testGetPermissions() {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			HibRole role = role();
			HibNode node = folder("news");
			assertEquals(6, roleDao.getPermissions(role, node).size());
		}
	}

	@Test
	public void testRevokePermission() {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
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
	public void testRevokePermissionOnGroupRoot() throws Exception {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			UserDaoWrapper userDao = tx.userDao();
			roleDao.revokePermissions(role(), meshRoot().getGroupRoot(), CREATE_PERM);
			HibUser user = user();
			assertFalse("The create permission to the groups root node should have been revoked.", userDao.hasPermission(user, meshRoot().getGroupRoot(), CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			long nRolesBefore = root.computeCount();

			final String roleName = "test2";
			RoleDaoWrapper roleDao = tx.roleDao();
			HibRole role = roleDao.create(roleName, user());
			assertNotNull(role);
			long nRolesAfter = root.computeCount();
			assertEquals(nRolesBefore + 1, nRolesAfter);
		}
	}

	@Test
	public void testRoleAddCrudPermissions() {
		try (Tx tx = tx()) {
			NodeDaoWrapper nodeDao = tx.nodeDao();
			RoleDaoWrapper roleDao = tx.roleDao();
			UserDaoWrapper userDao = tx.userDao();
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
			RoleDaoWrapper roleDao = tx.roleDao();
			GroupDaoWrapper groupDao = tx.groupDao();

			HibRole extraRole = roleDao.create("extraRole", user());
			groupDao.addRole(group(), extraRole);

			// Multiple add role calls should not affect the result
			groupDao.addRole(group(), extraRole);
			groupDao.addRole(group(), extraRole);
			groupDao.addRole(group(), extraRole);
			groupDao.addRole(group(), extraRole);

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
			assertNotNull(boot().roleRoot().findByName(role().getName()));
			assertNull(boot().roleRoot().findByName("bogus"));
		}
	}

	@Test
	@Override
	public void testFindByUUID() {
		try (Tx tx = tx()) {
			HibRole role = tx.roleDao().findByUuid(role().getUuid());
			assertNotNull(role);
			role = boot().roleRoot().findByUuid("bogus");
			assertNull(role);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			
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
			RoleDaoWrapper roleDao = tx.roleDao();

			HibRole role = roleDao.create(roleName, user());
			String uuid = role.getUuid();
			role = roleDao.findByUuid(uuid);
			assertNotNull(role);
			BulkActionContext context = createBulkContext();
			roleDao.delete(role, context);
			HibRole foundRole = roleDao.findByUuid(uuid);
			assertNull(foundRole);
		}

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			RoleDaoWrapper roleDao = tx.roleDao();

			MeshRoot root = meshRoot();
			InternalActionContext ac = mockActionContext();
			HibRole role = roleDao.create("SuperUser", user());
			assertFalse(userDao.hasPermission(user(), role, InternalPermission.CREATE_PERM));
			userDao.inheritRolePermissions(user(), root.getUserRoot(), role);
			ac.data().clear();
			assertTrue(userDao.hasPermission(user(), role, InternalPermission.CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			long size = Iterators.size(boot().roleRoot().findAll().iterator());
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
				RoleDaoWrapper roleDao = tx2.roleDao();
				HibRole role = role();
				uuid = role.getUuid();
				roleDao.delete(role, context);
				tx2.success();
			}
			assertElement(boot().roleRoot(), uuid, false);
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
