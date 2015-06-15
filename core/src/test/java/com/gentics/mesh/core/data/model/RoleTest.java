package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.core.data.model.auth.PermissionType.CREATE;
import static com.gentics.mesh.core.data.model.auth.PermissionType.DELETE;
import static com.gentics.mesh.core.data.model.auth.PermissionType.READ;
import static com.gentics.mesh.core.data.model.auth.PermissionType.UPDATE;
import static com.gentics.mesh.util.TinkerpopUtils.count;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.vertx.ext.apex.RoutingContext;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.tinkerpop.GraphPermission;
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
		//		try (Transaction tx = graphDb.beginTx()) {
		//			tx.success();
		//		}
		role = roleService.findOne(role.getId());
		assertNotNull(role);
		assertEquals(roleName, role.getName());
	}

	@Test
	public void testGrantPermission() {
		Role role = info.getRole();
		MeshNode content = data().getContent("news overview");
		MeshNode content2;
		//		try (Transaction tx = graphDb.beginTx()) {
		roleService.addPermission(role, content, CREATE, READ, UPDATE, DELETE);

		// content2
		content2 = nodeService.create();
		content2.setContent( data().getEnglish(), "Test");
		roleService.addPermission(role, content2, READ, DELETE);
		roleService.addPermission(role, content2, CREATE);
		//			tx.success();
		//		}
		GraphPermission permission = roleService.getGraphPermission(role, content2);
		assertNotNull(permission);
		assertTrue(permission.isPermitted(CREATE));
		assertTrue(permission.isPermitted(READ));
		assertTrue(permission.isPermitted(DELETE));
		assertFalse(permission.isPermitted(UPDATE));
		roleService.addPermission(role, role, CREATE);
	}

	@Test
	public void testIsPermitted() throws Exception {
		User user = info.getUser();
		MeshPermission perm = new MeshPermission(data().getFolder("news"), PermissionType.READ);
		long start = System.currentTimeMillis();
		int nRuns = 200000;
		for (int i = 0; i < nRuns; i++) {
			userService.isPermitted(user.getId(), perm);
		}
		long dur = System.currentTimeMillis() - start;
		System.out.println("Duration: " + dur / (double) nRuns);
	}

	@Test
	public void testGrantPermissionTwice() {
		Role role = info.getRole();
		MeshNode content = data().getContent("news overview");

		//		try (Transaction tx = graphDb.beginTx()) {
		roleService.addPermission(role, content, CREATE);
		//			tx.success();
		//		}

		//		try (Transaction tx = graphDb.beginTx()) {
		roleService.addPermission(role, content, CREATE);
		//			tx.success();
		//		}

		GraphPermission permission = roleService.getGraphPermission(role, content);
		assertNotNull(permission);
		assertTrue(permission.isPermitted(CREATE));
		assertTrue(permission.isPermitted(READ));
		assertTrue(permission.isPermitted(DELETE));
		assertTrue(permission.isPermitted(UPDATE));
	}

	@Test
	public void testRevokePermission() {
		Role role = info.getRole();
		MeshNode content = data().getContent("news overview");
		//		try (Transaction tx = graphDb.beginTx()) {
		GraphPermission permission = roleService.revokePermission(role, content, CREATE);
		assertFalse(permission.isPermitted(CREATE));
		assertTrue(permission.isPermitted(DELETE));
		assertTrue(permission.isPermitted(UPDATE));
		assertTrue(permission.isPermitted(READ));
		//			tx.success();
		//		}
	}

	@Test
	public void testRevokePermissionOnGroupRoot() throws Exception {

		//		try (Transaction tx = graphDb.beginTx()) {
		roleService.revokePermission(info.getRole(), data().getMeshRoot().getGroupRoot(), PermissionType.CREATE);
		//			tx.success();
		//		}

		assertFalse("The create permission to the groups root node should have been revoked.",
				userService.isPermitted(info.getUser().getId(), new MeshPermission(data().getMeshRoot().getGroupRoot(), PermissionType.CREATE)));

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
		//		try (Transaction tx = graphDb.beginTx()) {
		info.getGroup().addRole(extraRole);
		roleService.addPermission(info.getRole(), extraRole, PermissionType.READ);
		//			tx.success();
		//		}

		RoutingContext rc = getMockedRoutingContext("");
		Page<? extends Role> roles = roleService.findByGroup(rc, info.getGroup(), new PagingInfo(1, 10));
		assertEquals(2, roles.getSize());
		//assertEquals(2, roles.getTotalElements());
	}
}
