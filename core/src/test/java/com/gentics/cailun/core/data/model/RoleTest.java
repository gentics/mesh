package com.gentics.cailun.core.data.model;

import static com.gentics.cailun.core.data.model.auth.PermissionType.CREATE;
import static com.gentics.cailun.core.data.model.auth.PermissionType.DELETE;
import static com.gentics.cailun.core.data.model.auth.PermissionType.READ;
import static com.gentics.cailun.core.data.model.auth.PermissionType.UPDATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.repository.RoleRepository;
import com.gentics.cailun.demo.UserInfo;
import com.gentics.cailun.test.AbstractDBTest;

public class RoleTest extends AbstractDBTest {

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private ContentService contentService;

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
		Content content = data().getNews2015Content();
		Content content2;
		try (Transaction tx = graphDb.beginTx()) {
			roleService.addPermission(role, content, CREATE, READ, UPDATE, DELETE);

			// content2
			content2 = new Content();
			contentService.addI18NContent(content2, data().getEnglish(), "Test");
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
	public void testRevokePermission() {
		Role role = info.getRole();
		Content content = data().getNews2015Content();
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
}
