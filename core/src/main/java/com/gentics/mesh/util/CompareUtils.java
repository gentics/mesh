package com.gentics.mesh.util;

public final class CompareUtils {

	/**
	 * Returns {@code true} if the arguments are equal to each other and {@code false} otherwise. Consequently, if both arguments are {@code null}, {@code true}
	 * is returned and if exactly one argument is {@code null}, {@code
	 * false} is returned. Otherwise, equality is determined by using the {@link Object#equals equals} method of the first argument.
	 *
	 * @param a
	 *            an object
	 * @param b
	 *            an object to be compared with {@code a} for equality
	 * @return {@code true} if the arguments are equal to each other and {@code false} otherwise
	 * @see Object#equals(Object)
	 */
	public static boolean equals(Object a, Object b) {
		return (a == null && b == null) || (a != null && a.equals(b));
	}

}
