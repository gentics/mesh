package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PropertyUtilTest {

	@Test
	public void testResolve() {
		String in = "my ${MESH_DB_NETWORK_HOST} value";
		System.setProperty("MESH_DB_NETWORK_HOST", "great");
		String out = PropertyUtil.resolve(in);
		assertEquals("my great value", out);

		in = "my $MESH_DB value";
		System.setProperty("MESH_DB", "great2");
		out = PropertyUtil.resolve(in);
		assertEquals("my great2 value", out);
	}
}
