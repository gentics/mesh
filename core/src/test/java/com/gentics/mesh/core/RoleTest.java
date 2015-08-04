package com.gentics.mesh.core;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RoutingContextHelper;

public class RoleTest extends AbstractBasicObjectTest {

	@Test
	@Override
	public void testCreate() {
		String roleName = "test";
		RoleRoot root = meshRoot().getRoleRoot();
		Role createdRole = root.create(roleName, null, user());
		assertNotNull(createdRole);
		String uuid = createdRole.getUuid();
		boot.roleRoot().findByUuid(uuid, rh -> {
			Role role = rh.result();
			assertNotNull(role);
			assertEquals(roleName, role.getName());
		});
	}

	@Test
	public void testGrantPermission() {
		Role role = role();
		Node node = content("news overview");
		role.addPermissions(node, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);

		// node2
		Node parentNode = folder("2015");
		Node node2 = parentNode.create(user(), getSchemaContainer(), project());
//		NodeFieldContainer englishContainer = node2.getFieldContainer(english());
		// englishContainer.setI18nProperty("content", "Test");
		role.addPermissions(node2, READ_PERM, DELETE_PERM);
		role.addPermissions(node2, CREATE_PERM);
		Set<Permission> permissions = role.getPermissions(node2);

		assertNotNull(permissions);
		assertTrue(permissions.contains(CREATE_PERM));
		assertTrue(permissions.contains(READ_PERM));
		assertTrue(permissions.contains(DELETE_PERM));
		assertFalse(permissions.contains(UPDATE_PERM));
		role.addPermissions(role, CREATE_PERM);
	}

	@Test
	public void testIsPermitted() throws Exception {
		User user = user();
		int nRuns = 2000;
		for (int i = 0; i < nRuns; i++) {
			user.hasPermission(folder("news"), READ_PERM);
		}
	}

	@Test
	public void testGrantPermissionTwice() {
		Role role = role();
		Node node = content("news overview");

		role.addPermissions(node, CREATE_PERM);
		role.addPermissions(node, CREATE_PERM);

		Set<Permission> permissions = role.getPermissions(node);
		assertNotNull(permissions);
		assertTrue(permissions.contains(CREATE_PERM));
		assertTrue(permissions.contains(READ_PERM));
		assertTrue(permissions.contains(DELETE_PERM));
		assertTrue(permissions.contains(UPDATE_PERM));
	}

	@Test
	public void testGetPermissions() {
		Role role = role();
		Node node = content("news overview");
		assertEquals(4, role.getPermissions(node).size());
	}

	@Test
	public void testRevokePermission() {
		Role role = role();
		Node node = content("news overview");
		role.revokePermissions(node, CREATE_PERM);

		Set<Permission> permissions = role.getPermissions(node);
		assertNotNull(permissions);
		assertFalse(permissions.contains(CREATE_PERM));
		assertTrue(permissions.contains(DELETE_PERM));
		assertTrue(permissions.contains(UPDATE_PERM));
		assertTrue(permissions.contains(READ_PERM));
	}

	@Test
	public void testRevokePermissionOnGroupRoot() throws Exception {

		role().revokePermissions(meshRoot().getGroupRoot(), CREATE_PERM);
		User user = user();
		assertFalse("The create permission to the groups root node should have been revoked.",
				user.hasPermission(meshRoot().getGroupRoot(), CREATE_PERM));
	}

	@Test
	@Override
	public void testRootNode() {
		RoleRoot root = meshRoot().getRoleRoot();
		int nRolesBefore = root.findAll().size();

		final String roleName = "test2";
		Role role = root.create(roleName, null, user());
		assertNotNull(role);
		int nRolesAfter = root.findAll().size();
		assertEquals(nRolesBefore + 1, nRolesAfter);

	}

	@Test
	public void testRoleAddCrudPermissions() {

		MeshAuthUser requestUser = user().getImpl().reframe(MeshAuthUserImpl.class);
		// userRoot.findMeshAuthUserByUsername(requestUser.getUsername())
		Node parentNode = folder("2015");
		assertNotNull(parentNode);

		// Also assign create permissions on the parent object to other roles
		for (Role role : roles().values()) {
			role.addPermissions(parentNode, CREATE_PERM);
		}

		Node node = parentNode.create(user(), getSchemaContainer(), project());
		assertEquals(0, requestUser.getPermissions(node).size());
		requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);
		assertEquals(4, requestUser.getPermissions(node).size());

		for (Role role : roles().values()) {
			for (Permission permission : Permission.values()) {
				assertTrue(role.hasPermission(permission, node));
			}
		}
	}

	@Test
	public void testRolesOfGroup() throws InvalidArgumentException {

		RoleRoot root = meshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole", group(), user());
		
		// Multiple add role calls should not affect the result
		group().addRole(extraRole);
		group().addRole(extraRole);
		group().addRole(extraRole);
		group().addRole(extraRole);
		
		role().addPermissions(extraRole, READ_PERM);

		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = RoutingContextHelper.getUser(rc);
		Page<? extends Role> roles = group().getRoles(requestUser, new PagingInfo(1, 10));
		assertEquals(2, roles.getSize());
		// assertEquals(2, roles.getTotalElements());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = RoutingContextHelper.getUser(rc);
		Page<? extends Role> page = boot.roleRoot().findAll(requestUser, new PagingInfo(1, 10));
		assertEquals(roles().size(), page.getTotalElements());
		assertEquals(10, page.getSize());

		page = boot.roleRoot().findAll(requestUser, new PagingInfo(1, 15));
		assertEquals(roles().size(), page.getTotalElements());
		assertEquals(15, page.getSize());
	}

	@Test
	@Override
	public void testFindByName() {
		assertNotNull(boot.roleRoot().findByName(role().getName()));
		assertNull(boot.roleRoot().findByName("bogus"));
	}

	@Test
	@Override
	public void testFindByUUID() {
		boot.roleRoot().findByUuid(role().getUuid(), rh -> {
			assertNotNull(rh.result());
		});
		boot.roleRoot().findByUuid("bogus", rh -> {
			assertNull(rh.result());
		});
	}

	@Test
	@Override
	public void testTransformation() throws InterruptedException {
		Role role = role();
		CountDownLatch latch = new CountDownLatch(1);
		RoutingContext rc = getMockedRoutingContext("");
		role.transformToRest(rc, rh -> {
			RoleResponse restModel = rh.result();
			assertNotNull(restModel);
			assertEquals(role.getName(), restModel.getName());
			assertEquals(role.getUuid(), restModel.getUuid());
			latch.countDown();
		});
		latch.await();

	}

	@Test
	@Override
	public void testCreateDelete() {
		String roleName = "test";
		RoleRoot root = meshRoot().getRoleRoot();

		Role role = root.create(roleName, null, user());
		String uuid = role.getUuid();
		boot.roleRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
			role.delete();
			boot.roleRoot().findByUuid(uuid, rh2 -> {
				assertNull(rh2.result());
			});
		});

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		MeshRoot root = meshRoot();
		Role role = root.getRoleRoot().create("SuperUser", null, user());
		assertFalse(user().hasPermission(role, Permission.CREATE_PERM));
		user().addCRUDPermissionOnRole(root.getUserRoot(), Permission.CREATE_PERM, role);
		assertTrue(user().hasPermission(role, Permission.CREATE_PERM));
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		List<? extends Role> roles = boot.roleRoot().findAll();
		assertNotNull(roles);
		assertEquals(roles().size(), roles.size());
	}

	@Test
	@Override
	public void testRead() {
		Role role = role();
		assertEquals("joe1_role", role.getName());
		assertNotNull(role.getUuid());

		assertNotNull(role.getCreationTimestamp());
		assertNotNull(role.getCreator());

		assertNotNull(role.getEditor());
		assertNotNull(role.getLastEditedTimestamp());
	}

	@Test
	@Override
	public void testDelete() {
		Role role = role();
		String uuid = role.getUuid();
		role.delete();
		boot.roleRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
		});
	}

	@Test
	@Override
	public void testUpdate() {
		Role role = role();
		role.setName("newName");
		assertEquals("newName", role.getName());
		// assertEquals(1,role.getProjects());
		// TODO test project assignments
	}

	@Test
	@Override
	public void testReadPermission() {
		testPermission(Permission.READ_PERM, role());
	}

	@Test
	@Override
	public void testDeletePermission() {
		testPermission(Permission.DELETE_PERM, role());
	}

	@Test
	@Override
	public void testUpdatePermission() {
		testPermission(Permission.UPDATE_PERM, role());
	}

	@Test
	@Override
	public void testCreatePermission() {
		testPermission(Permission.CREATE_PERM, role());
	}
}
