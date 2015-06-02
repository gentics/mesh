package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.core.data.model.auth.PermissionType.CREATE;
import static com.gentics.mesh.core.data.model.auth.PermissionType.DELETE;
import static com.gentics.mesh.core.data.model.auth.PermissionType.READ;
import static com.gentics.mesh.core.data.model.auth.PermissionType.UPDATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import com.gentics.mesh.core.data.model.auth.GraphPermission;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.auth.Role;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.UserService;
import com.gentics.mesh.core.repository.RoleRepository;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.paging.MeshPageRequest;
import com.gentics.mesh.test.AbstractDBTest;

public class RoleTest extends AbstractDBTest {

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private MeshNodeService contentService;

	@Autowired
	private UserService userService;

	private UserInfo info;

	@Before
	public void setup() throws Exception {
		setupData();
		info = data().getUserInfo();
	}

	@Test
	public void testCreation() {
		final String roleName = "test";
		Role role = new Role(roleName);
		try (Transaction tx = graphDb.beginTx()) {
			roleRepository.save(role);
			tx.success();
		}
		role = roleRepository.findOne(role.getId());
		assertNotNull(role);
		assertEquals(roleName, role.getName());
	}

	@Test
	public void testGrantPermission() {
		Role role = info.getRole();
		MeshNode content = data().getContent("news overview");
		MeshNode content2;
		try (Transaction tx = graphDb.beginTx()) {
			roleService.addPermission(role, content, CREATE, READ, UPDATE, DELETE);

			// content2
			content2 = new MeshNode();
			contentService.setContent(content2, data().getEnglish(), "Test");
			content2 = contentService.save(content2);
			roleService.addPermission(role, content2, READ, DELETE);
			roleService.addPermission(role, content2, CREATE);
			tx.success();
		}
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

		try (Transaction tx = graphDb.beginTx()) {
			roleService.addPermission(role, content, CREATE);
			tx.success();
		}

		try (Transaction tx = graphDb.beginTx()) {
			roleService.addPermission(role, content, CREATE);
			tx.success();
		}

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
		try (Transaction tx = graphDb.beginTx()) {
			GraphPermission permission = roleService.revokePermission(role, content, CREATE);
			assertFalse(permission.isPermitted(CREATE));
			assertTrue(permission.isPermitted(DELETE));
			assertTrue(permission.isPermitted(UPDATE));
			assertTrue(permission.isPermitted(READ));
			tx.success();
		}
	}

	@Test
	public void testRevokePermissionOnGroupRoot() throws Exception {

		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), data().getMeshRoot().getGroupRoot(), PermissionType.CREATE);
			tx.success();
		}

		assertFalse("The create permission to the groups root node should have been revoked.",
				userService.isPermitted(info.getUser().getId(), new MeshPermission(data().getMeshRoot().getGroupRoot(), PermissionType.CREATE)));

	}

	@Test
	public void testRoleRoot() {
		int nRolesBefore = roleRepository.findRoot().getRoles().size();

		final String roleName = "test2";
		try (Transaction tx = graphDb.beginTx()) {
			Role role = new Role(roleName);
			roleRepository.save(role);
			tx.success();
		}

		int nRolesAfter = roleRepository.findRoot().getRoles().size();
		assertEquals(nRolesBefore + 1, nRolesAfter);

	}

	@Test
	public void testRolesOfGroup() {

		Role extraRole = new Role("extraRole");
		try (Transaction tx = graphDb.beginTx()) {
			extraRole = roleService.save(extraRole);
			info.getGroup().addRole(extraRole);
			groupService.save(info.getGroup());
			roleService.addPermission(info.getRole(), extraRole, PermissionType.READ);
			tx.success();
		}

		Page<Role> roles = roleRepository.findByGroup(info.getUser().getUuid(), info.getGroup(), new MeshPageRequest(0, 10));
		assertEquals(2, roles.getTotalElements());
	}
}
