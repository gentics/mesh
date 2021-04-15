package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PropertyUtilTest {

	@Test
	public void testResolve() {
		String in = "my ${ORIENTDB_NETWORK_HOST} value";
		System.setProperty("ORIENTDB_NETWORK_HOST", "great");
		String out = PropertyUtil.resolve(in);
		assertEquals("my great value", out);

		in = "my $ORIENTDB value";
		System.setProperty("ORIENTDB", "great2");
		out = PropertyUtil.resolve(in);
		assertEquals("my great2 value", out);
	}
}
