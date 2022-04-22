package com.gentics.mesh.test;

import java.math.BigDecimal;

import org.assertj.core.data.Percentage;

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
		org.assertj.core.api.Assertions.assertThat(new BigDecimal(actual.toString()).doubleValue()).isCloseTo(new BigDecimal(expected.toString()).doubleValue(), Percentage.withPercentage(0.999999));
	}

}
