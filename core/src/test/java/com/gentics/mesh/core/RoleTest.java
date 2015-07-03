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
import static org.junit.Assert.fail;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RoutingContextHelper;

public class RoleTest extends AbstractBasicObjectTest {

	@Test
	@Override
	public void testCreate() {
		String roleName = "test";
		RoleRoot root = data().getMeshRoot().getRoleRoot();
		Role role = root.create(roleName);
		role = boot.roleRoot().findByUUID(role.getUuid());
		assertNotNull(role);
		assertEquals(roleName, role.getName());
	}

	@Test
	public void testGrantPermission() {
		Role role = getRole();
		Node node = data().getContent("news overview");
		role.addPermissions(node, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);

		// node2
		Node parentNode = data().getFolder("2015");
		Node node2 = parentNode.create();
		NodeFieldContainer englishContainer = node2.getFieldContainer(data().getEnglish());
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
		User user = getUser();
		int nRuns = 2000;
		for (int i = 0; i < nRuns; i++) {
			user.hasPermission(data().getFolder("news"), READ_PERM);
		}
	}

	@Test
	public void testGrantPermissionTwice() {
		Role role = getRole();
		Node node = data().getContent("news overview");

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
		Role role = getRole();
		Node node = data().getContent("news overview");
		assertEquals(4, role.getPermissions(node).size());
	}

	@Test
	public void testRevokePermission() {
		Role role = getRole();
		Node node = data().getContent("news overview");
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

		getRole().revokePermissions(data().getMeshRoot().getGroupRoot(), CREATE_PERM);
		User user = getUser();
		assertFalse("The create permission to the groups root node should have been revoked.", user.hasPermission(data().getMeshRoot(), CREATE_PERM));
	}

	@Test
	@Override
	public void testRootNode() {
		RoleRoot root = data().getMeshRoot().getRoleRoot();
		int nRolesBefore = root.findAll().size();

		final String roleName = "test2";
		Role role = root.create(roleName);
		assertNotNull(role);
		int nRolesAfter = root.findAll().size();
		assertEquals(nRolesBefore + 1, nRolesAfter);

	}

	@Test
	public void testRoleAddCrudPermissions() {

		MeshAuthUser requestUser = getUser().getImpl().reframe(MeshAuthUserImpl.class);
		// userRoot.findMeshAuthUserByUsername(requestUser.getUsername())
		Node parentNode = data().getFolder("2015");
		assertNotNull(parentNode);

		// Also assign create permissions on the parent object to other roles
		for (Role role : data().getRoles().values()) {
			role.addPermissions(parentNode, CREATE_PERM);
		}

		Node node = parentNode.create();
		assertEquals(0, requestUser.getPermissions(node).size());
		requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);
		assertEquals(4, requestUser.getPermissions(node).size());

		for (Role role : data().getRoles().values()) {
			for (Permission permission : Permission.values()) {
				assertTrue(role.hasPermission(permission, node));
			}
		}
	}

	@Test
	public void testRolesOfGroup() throws InvalidArgumentException {

		RoleRoot root = data().getMeshRoot().getRoleRoot();
		Role extraRole = root.create("extraRole");
		getGroup().addRole(extraRole);
		getRole().addPermissions(extraRole, READ_PERM);

		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = RoutingContextHelper.getUser(rc);
		Page<? extends Role> roles = getGroup().getRoles(requestUser, new PagingInfo(1, 10));
		assertEquals(2, roles.getSize());
		// assertEquals(2, roles.getTotalElements());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = RoutingContextHelper.getUser(rc);
		Page<? extends Role> page = boot.roleRoot().findAll(requestUser, new PagingInfo(1, 10));
		assertEquals(data().getUsers().size(), page.getTotalElements());
		assertEquals(10, page.getSize());

		page = boot.roleRoot().findAll(requestUser, new PagingInfo(1, 15));
		assertEquals(data().getUsers().size(), page.getTotalElements());
		assertEquals(15, page.getSize());
	}

	@Test
	@Override
	public void testFindByName() {
		assertNotNull(boot.roleRoot().findByName(getRole().getName()));
		assertNull(boot.roleRoot().findByName("bogus"));
	}

	@Test
	@Override
	public void testFindByUUID() {
		assertNotNull(boot.roleRoot().findByUUID(getRole().getUuid()));
		assertNull(boot.roleRoot().findByUUID("bogus"));
	}

	@Test
	@Override
	public void testTransformation() {
		Role role = getRole();
		RoleResponse restModel = role.transformToRest();
		assertNotNull(restModel);

		assertEquals(role.getName(), restModel.getName());
		assertEquals(role.getUuid(), restModel.getUuid());
	}

	@Test
	@Override
	public void testCreateDelete() {
		fail("not yet implemented");
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		fail("not yet implemented");
	}

	@Test
	@Override
	public void testPermissionsOnObject() {
		fail("not yet implemented");
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		List<? extends Role> roles = boot.roleRoot().findAll();
		assertNotNull(roles);
		assertEquals(data().getRoles().size(), roles.size());
	}

	@Test
	@Override
	public void testRead() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testDelete() {
		Role role = getRole();
		String uuid = role.getUuid();
		role.delete();
		assertNull(boot.roleRoot().findByUUID(uuid));
	}

	@Test
	@Override
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testReadPermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testDeletePermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testUpdatePermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreatePermission() {
		fail("Not yet implemented");
	}
}
