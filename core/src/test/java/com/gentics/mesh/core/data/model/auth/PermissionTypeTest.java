package com.gentics.mesh.core.data.model.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.model.auth.PermissionType;

public class PermissionTypeTest {

	@Test
	public void testPermissionTypeTransformation() {
		String perm = "read";
		PermissionType permType = PermissionType.fromString(perm);
		assertEquals(PermissionType.READ, permType);
	}
}
