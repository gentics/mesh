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
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.repository.RoleRepository;
import com.gentics.cailun.test.AbstractDBTest;
import com.gentics.cailun.test.UserInfo;

public class RoleTest extends AbstractDBTest {

	@Autowired
	private RoleRepository roleRepository;

	private UserInfo info;

	@Before
	public void setup() {
		setupData();
		info = data().getUserInfo();

	}

	@Test
	public void testCreation() {
		final String roleName = "test";
		Role role = new Role(roleName);
		roleRepository.save(role);
		role = roleRepository.findOne(role.getId());
		assertNotNull(role);
		assertEquals(roleName, role.getName());
	}

	@Test
	public void testGrantPermission() {
		Role role = info.getRole();
		Content content = data().getContentLevel1A1();
		Content content2 = data().getContentLevel1A2();
		roleService.addPermission(role, content, CREATE, READ, UPDATE, DELETE);
		roleService.addPermission(role, content2, READ, DELETE);
		roleService.addPermission(role, content2, CREATE);
		GraphPermission permission = roleService.getGraphPermission(role, content2);
		assertNotNull(permission);
		assertTrue(permission.isPermitted(CREATE));
		assertTrue(permission.isPermitted(READ));
		assertTrue(permission.isPermitted(DELETE));
		assertFalse(permission.isPermitted(UPDATE));
		roleService.addPermission(role, role, CREATE);

	}
}
