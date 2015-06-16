package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.core.data.model.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.util.TinkerpopUtils.count;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.vertx.ext.web.RoutingContext;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.auth.MeshPermission;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.relationship.Permission;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.core.data.model.tinkerpop.User;
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
		Role role = roleService.create(roleName);
		role = roleService.findOne(role.getId());
		assertNotNull(role);
		assertEquals(roleName, role.getName());
	}

	@Test
	public void testGrantPermission() {
		Role role = info.getRole();
		MeshNode content = data().getContent("news overview");
		MeshNode content2;
		role.addPermissions(content, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);

		// content2
		content2 = nodeService.create();
		content2.setContent(data().getEnglish(), "Test");
		role.addPermissions(content2, READ_PERM, DELETE_PERM);
		role.addPermissions(content2, CREATE_PERM);
		Set<Permission> permissions = role.getPermissions(content2);

		assertNotNull(permissions);
		assertTrue(permissions.contains(CREATE_PERM));
		assertTrue(permissions.contains(READ_PERM));
		assertTrue(permissions.contains(DELETE_PERM));
		assertFalse(permissions.contains(UPDATE_PERM));
		role.addPermissions(role, CREATE_PERM);
	}

	@Test
	public void testIsPermitted() throws Exception {
		User user = info.getUser();
		MeshPermission perm = new MeshPermission(data().getFolder("news"), READ_PERM);
		long start = System.currentTimeMillis();
		int nRuns = 200000;
		for (int i = 0; i < nRuns; i++) {
			user.
			userService.isPermitted(user.getId(), perm);
		}
		long dur = System.currentTimeMillis() - start;
		System.out.println("Duration: " + dur / (double) nRuns);
	}

	@Test
	public void testGrantPermissionTwice() {
		Role role = info.getRole();
		MeshNode content = data().getContent("news overview");

		role.addPermissions(content, CREATE_PERM);
		role.addPermissions(content, CREATE_PERM);

		Set<Permission> permissions = role.getPermissions(content);
		assertNotNull(permissions);
		assertTrue(permissions.contains(CREATE_PERM));
		assertTrue(permissions.contains(READ_PERM));
		assertTrue(permissions.contains(DELETE_PERM));
		assertTrue(permissions.contains(UPDATE_PERM));
	}

	@Test
	public void testRevokePermission() {
		Role role = info.getRole();
		MeshNode content = data().getContent("news overview");
		role.revokePermissions(content, CREATE_PERM);

		Set<Permission> permissions = role.getPermissions(content);
		assertNotNull(permissions);
		assertFalse(permissions.contains(CREATE_PERM));
		assertTrue(permissions.contains(DELETE_PERM));
		assertTrue(permissions.contains(UPDATE_PERM));
		assertTrue(permissions.contains(READ_PERM));
	}

	@Test
	public void testRevokePermissionOnGroupRoot() throws Exception {

		info.getRole().revokePermissions(data().getMeshRoot().getGroupRoot(), CREATE_PERM);

		assertFalse("The create permission to the groups root node should have been revoked.",
				userService.isPermitted(info.getUser().getId(), new MeshPermission(data().getMeshRoot().getGroupRoot(), CREATE_PERM)));

	}

	@Test
	public void testRoleRoot() {
		int nRolesBefore = count(roleService.findRoot().getRoles());

		final String roleName = "test2";
		Role role = roleService.create(roleName);
		assertNotNull(role);
		int nRolesAfter = count(roleService.findRoot().getRoles());
		assertEquals(nRolesBefore + 1, nRolesAfter);

	}

	@Test
	public void testRolesOfGroup() throws InvalidArgumentException {

		Role extraRole = roleService.create("extraRole");
		info.getGroup().addRole(extraRole);
		info.getRole().addPermissions(extraRole, READ_PERM);

		RoutingContext rc = getMockedRoutingContext("");
		Page<? extends Role> roles = roleService.findByGroup(rc, info.getGroup(), new PagingInfo(1, 10));
		assertEquals(2, roles.getSize());
		//assertEquals(2, roles.getTotalElements());
	}
}
