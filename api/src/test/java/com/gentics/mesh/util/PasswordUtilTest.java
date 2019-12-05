package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class PasswordUtilTest {

	@Test
	public void testHumanPassword() {
		String pw = PasswordUtil.humanPassword(10);
		assertEquals(pw.length(), 10);
		assertNotEquals(pw, PasswordUtil.humanPassword(10));
		assertEquals("", PasswordUtil.humanPassword(0));
	}

	@Test
	public void testExcludedChars() {
		List<Character> disallowedChars = Arrays.asList('Q', '8', 'B', 'Z', '0', 'O', 'o', '1', 'i', 'I', '5', 'S', 's');
		for (int i = 0; i < 10000; i++) {
			String pw = PasswordUtil.humanPassword(20);
			for (int e = 0; e < pw.length(); e++) {
				char pwChar = pw.charAt(e);
				assertFalse("Found disallowed character in pw {" + pw + "} - {" + pwChar + "}", disallowedChars.contains(pwChar));
			}
		}
	}
}
