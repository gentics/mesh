package com.gentics.mesh.test;

import java.math.BigDecimal;

/**
 * Extended {@link org.junit.Assert} class which contains additional asserters.
 */
public class Assert extends org.junit.Assert {

	/**
	 * Assert that the numeric values of two numbers are equal.
	 * 
	 * @param expected
	 *            the expected value
	 * @param actual
	 *            the actual value
	 */
	public static void assertNumberValueEquals(Number expected, Number actual) {
		if (expected == null || actual == null) {
			fail("Expected both numbers to be not null.");
		}
		// we convert both numbers to big decimal and compare them
		assertEquals(new BigDecimal(expected.toString()), new BigDecimal(actual.toString()));
	}

}
