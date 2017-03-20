package com.gentics.mesh.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class TokenUtilTest {

	@Test
	public void testTokenGeneration() {
		String token = TokenUtil.randomToken();
		assertNotNull(token);
		assertFalse(token.isEmpty());
		System.out.println(token);
	}
}
