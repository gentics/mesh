package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.repository.RoleRepository;
import com.gentics.cailun.test.AbstractDBTest;

public class RoleTest extends AbstractDBTest {

	@Autowired
	RoleRepository roleRepository;

	@Test
	public void testCreation() {
		final String roleName = "test";
		Role role = new Role(roleName);
		roleRepository.save(role);
		role = roleRepository.findOne(role.getId());
		assertNotNull(role);
		assertEquals(roleName, role.getName());
	}
}
