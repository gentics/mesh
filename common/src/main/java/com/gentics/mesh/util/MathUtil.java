package com.gentics.mesh.util;

/**
 * Mathematical utils
 */
public final class MathUtil {

	private MathUtil() {

	}

	public static long ceilDiv(long x, long y) {
		long r = x / y;
		// if the signs are different and modulo not zero, round up
		if ((x ^ y) > 0 && (r * y != x)) {
			r++;
		}
		return r;
	}
}
