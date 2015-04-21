package com.gentics.cailun.core.data.model.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PermissionTypeTest {

	@Test
	public void testPermissionTypeTransformation() {
		String perm = "read";
		PermissionType permType = PermissionType.fromString(perm);
		assertEquals(PermissionType.READ, permType);
	}
}
