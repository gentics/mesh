package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MathUtilTest {

	@Test
	public void testCeilDiv() {
		assertCeilDiv(1, 1, 1);
		assertCeilDiv(1, 2, 1);
		assertCeilDiv(2, 1, 2);
		assertCeilDiv(3, 1, 3);
		assertCeilDiv(1, 3, 1);
		assertCeilDiv(2, 3, 1);
	}

	private void assertCeilDiv(int i, int j, int expected) {
		assertEquals(expected, MathUtil.ceilDiv(i, j));
	}
}
