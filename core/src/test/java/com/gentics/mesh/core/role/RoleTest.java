package com.gentics.mesh.core.role;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
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
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
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
			RoleRoot root = meshRoot().getRoleRoot();
			Role createdRole = root.create(roleName, user());
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
			Role role = role();
			Node node = folder("news");
			role.grantPermissions(node, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);

			// node2
			Node parentNode = folder("2015");
			Node node2 = parentNode.create(user(), getSchemaContainer().getLatestVersion(), project());
			// NodeFieldContainer englishContainer = node2.getFieldContainer(english());
			// englishContainer.setI18nProperty("content", "Test");
			role.grantPermissions(node2, READ_PERM, DELETE_PERM);
			role.grantPermissions(node2, CREATE_PERM);
			Set<GraphPermission> permissions = role.getPermissions(node2);

			assertNotNull(permissions);
			assertTrue(permissions.contains(CREATE_PERM));
			assertTrue(permissions.contains(READ_PERM));
			assertTrue(permissions.contains(DELETE_PERM));
			assertFalse(permissions.contains(UPDATE_PERM));
			role.grantPermissions(role, CREATE_PERM);
		}
	}

	@Test
	public void testIsPermitted() throws Exception {
		try (Tx tx = tx()) {
			UserRoot userRoot = tx.data().userDao();
			User user = user();
			int nRuns = 2000;
			for (int i = 0; i < nRuns; i++) {
				userRoot.hasPermission(user, folder("news"), READ_PERM);
			}
		}
	}

	@Test
	public void testGrantPermissionTwice() {
		try (Tx tx = tx()) {
			Role role = role();
			Node node = folder("news");

			role.grantPermissions(node, CREATE_PERM);
			role.grantPermissions(node, CREATE_PERM);

			Set<GraphPermission> permissions = role.getPermissions(node);
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
			Role role = role();
			Node node = folder("news");
			assertEquals(6, role.getPermissions(node).size());
		}
	}

	@Test
	public void testRevokePermission() {
		try (Tx tx = tx()) {
			Role role = role();
			Node node = folder("news");
			role.revokePermissions(node, CREATE_PERM);

			Set<GraphPermission> permissions = role.getPermissions(node);
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
			UserRoot userRoot = tx.data().userDao();
			role().revokePermissions(meshRoot().getGroupRoot(), CREATE_PERM);
			User user = user();
			assertFalse("The create permission to the groups root node should have been revoked.", userRoot.hasPermission(user, meshRoot().getGroupRoot(), CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			long nRolesBefore = root.computeCount();

			final String roleName = "test2";
			Role role = root.create(roleName, user());
			assertNotNull(role);
			long nRolesAfter = root.computeCount();
			assertEquals(nRolesBefore + 1, nRolesAfter);
		}
	}

	@Test
	public void testRoleAddCrudPermissions() {
		try (Tx tx = tx()) {
			UserRoot userRoot = tx.data().userDao();
			MeshAuthUser requestUser = user().reframe(MeshAuthUserImpl.class);
			// userRoot.findMeshAuthUserByUsername(requestUser.getUsername())
			Node parentNode = folder("news");
			assertNotNull(parentNode);

			// Grant all permissions to all roles
			for (Role role : roles().values()) {
				for (GraphPermission perm : GraphPermission.values()) {
					role.grantPermissions(parentNode, perm);
				}
			}

			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			Node node = parentNode.create(user(), getSchemaContainer().getLatestVersion(), project());
			assertEquals(0, userRoot.getPermissions(requestUser, node).size());
			userRoot.inheritRolePermissions(requestUser, parentNode, node);
			ac.data().clear();
			assertEquals(6, userRoot.getPermissions(requestUser, node).size());

			try (Tx tx2 = tx()) {
				for (Role role : roles().values()) {
					for (GraphPermission permission : GraphPermission.values()) {
						assertTrue("The role {" + role.getName() + "} does not grant perm {" + permission.getRestPerm().getName() + "} to the node {"
							+ node.getUuid() + "} but it should since the parent object got this role permission.",
							role.hasPermission(permission,
								node));
					}
				}
			}
		}
	}

	@Test
	public void testRolesOfGroup() throws InvalidArgumentException {

		try (Tx tx = tx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", user());
			group().addRole(extraRole);

			// Multiple add role calls should not affect the result
			group().addRole(extraRole);
			group().addRole(extraRole);
			group().addRole(extraRole);
			group().addRole(extraRole);

			role().grantPermissions(extraRole, READ_PERM);
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			MeshAuthUser requestUser = ac.getUser();
			Page<? extends Role> roles = group().getRoles(requestUser, new PagingParametersImpl(1, 10L));
			assertEquals(2, roles.getSize());
			assertEquals(1, extraRole.getGroups().count());

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
			Page<? extends Role> page = boot().roleRoot().findAll(ac, new PagingParametersImpl(1, 5L));
			assertEquals(roleCount, page.getTotalElements());
			assertEquals(roleCount, page.getSize());

			page = boot().roleRoot().findAll(ac, new PagingParametersImpl(1, 15L));
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
			Role role = boot().roleRoot().findByUuid(role().getUuid());
			assertNotNull(role);
			role = boot().roleRoot().findByUuid("bogus");
			assertNull(role);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			Role role = role();
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			RoleResponse restModel = role.transformToRestSync(ac, 0);

			assertNotNull(restModel);
			assertEquals(role.getName(), restModel.getName());
			assertEquals(role.getUuid(), restModel.getUuid());
		}

	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (Tx tx = tx()) {
			String roleName = "test";
			RoleRoot root = meshRoot().getRoleRoot();

			Role role = root.create(roleName, user());
			String uuid = role.getUuid();
			role = boot().roleRoot().findByUuid(uuid);
			assertNotNull(role);
			BulkActionContext context = createBulkContext();
			role.delete(context);
			Role foundRole = boot().roleRoot().findByUuid(uuid);
			assertNull(foundRole);
		}

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			UserRoot userRoot = tx.data().userDao();
			MeshRoot root = meshRoot();
			InternalActionContext ac = mockActionContext();
			Role role = root.getRoleRoot().create("SuperUser", user());
			assertFalse(userRoot.hasPermission(user(), role, GraphPermission.CREATE_PERM));
			userRoot.inheritRolePermissions(user(), root.getUserRoot(), role);
			ac.data().clear();
			assertTrue(userRoot.hasPermission(user(), role, GraphPermission.CREATE_PERM));
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
			Role role = role();
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
				Role role = role();
				uuid = role.getUuid();
				role.delete(context);
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
			Role role = role();
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
			testPermission(GraphPermission.READ_PERM, role());
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.DELETE_PERM, role());
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.UPDATE_PERM, role());
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.CREATE_PERM, role());
		}
	}
}
