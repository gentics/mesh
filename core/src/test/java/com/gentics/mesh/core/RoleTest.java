package com.gentics.mesh.core;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.vertx.ext.web.RoutingContext;

import java.util.Set;

import org.apache.tools.ant.types.CommandlineJava.SysProperties;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class RoleTest extends AbstractDBTest {

	private UserInfo info;

	@Before
	public void setup() throws Exception {
		setupData();
		info = data().getUserInfo();
	}

	@Test
	public void testCreation() {
		final String roleName = "test";
		RoleRoot root = data().getMeshRoot().getRoleRoot();
		Role role = root.create(roleName);
		role = roleService.findByUUID(role.getUuid());
		assertNotNull(role);
		assertEquals(roleName, role.getName());
	}

	@Test
	public void testGrantPermission() {
		Role role = info.getRole();
		Node node = data().getContent("news overview");
		role.addPermissions(node, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);

		// node2
		Node parentNode = data().getFolder("2015");
		Node node2 = parentNode.create();
		NodeFieldContainer englishContainer = node2.getFieldContainer(data().getEnglish());
		//		englishContainer.setI18nProperty("content", "Test");
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
		MeshUser user = info.getUser();
		long start = System.currentTimeMillis();
		int nRuns = 200000;
		for (int i = 0; i < nRuns; i++) {
			user.hasPermission(data().getFolder("news"), READ_PERM);
		}
		long dur = System.currentTimeMillis() - start;
		System.out.println("Duration: " + dur / (double) nRuns);
	}

	@Test
	public void testGrantPermissionTwice() {
		Role role = info.getRole();
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
		Role role = info.getRole();
		Node node = data().getContent("news overview");
		assertEquals(4, role.getPermissions(node).size());
	}

	@Test
	public void testRevokePermission() {
		Role role = info.getRole();
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

		info.getRole().revokePermissions(data().getMeshRoot().getGroupRoot(), CREATE_PERM);
		MeshUser user = info.getUser();
		assertFalse("The create permission to the groups root node should have been revoked.", user.hasPermission(data().getMeshRoot(), CREATE_PERM));

	}

	@Test
	public void testRoleRoot() {
		RoleRoot root = data().getMeshRoot().getRoleRoot();
		int nRolesBefore = root.getRoles().size();

		final String roleName = "test2";
		Role role = root.create(roleName);
		assertNotNull(role);
		int nRolesAfter = root.getRoles().size();
		assertEquals(nRolesBefore + 1, nRolesAfter);

	}

	@Test
	public void testRoleAddCrudPermissions() {

		MeshAuthUser requestUser = info.getUser().getImpl().reframe(MeshAuthUserImpl.class);
		//userService.findMeshAuthUserByUsername(requestUser.getUsername())
		Node parentNode = data().getFolder("2015");
		assertNotNull(parentNode);

		// Also assign create permissions on the parent object to other roles
		for (Role role : data().getRoles().values()) {
			role.addPermissions(parentNode, CREATE_PERM);
		}

		Node node = parentNode.create();
		assertEquals(0, requestUser.getPermissions(node).size());
		roleService.addCRUDPermissionOnRole(requestUser, parentNode, CREATE_PERM, node);
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
		info.getGroup().addRole(extraRole);
		info.getRole().addPermissions(extraRole, READ_PERM);

		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		Page<? extends Role> roles = info.getGroup().getRoles(requestUser, new PagingInfo(1, 10));
		assertEquals(2, roles.getSize());
		// assertEquals(2, roles.getTotalElements());
	}
}
