package com.gentics.mesh.query.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.parameter.impl.RolePermissionParameters;

public class RolePermissionParametersTest {

	@Test
	public void testRoleParam() {
		RolePermissionParameters params = new RolePermissionParameters();
		assertNull(params.getRoleUuid());
		params.setRoleUuid("bogus");
		assertEquals("bogus", params.getRoleUuid());
		assertEquals("role=bogus", params.getQueryParameters());
	}
}
