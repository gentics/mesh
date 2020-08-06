package com.gentics.mesh.util;

import java.util.List;
import java.util.ListIterator;

public final class CompareUtils {

	/**
	 * Returns {@code true} if the arguments are equal to each other and {@code false} otherwise. Consequently, if both arguments are {@code null}, {@code true}
	 * is returned and if exactly one argument is {@code null}, {@code false} is returned. Otherwise, equality is determined by using the {@link Object#equals
	 * equals} method of the first argument.
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

	/**
	 * Returns {@code true} if the numbers lists are equal to each other and {@code false} otherwise. Consequently, if both arguments are {@code null},
	 * {@code true} is returned and if exactly one argument is {@code null}, {@code false} is returned. Otherwise, equality is determined by using the
	 * {@link NumberUtils#compare} method by comparing each list item.
	 *
	 * @param a
	 *            First number list
	 * @param b
	 *            Second number list to be compared with {@code a} for equality
	 * @return {@code true} if the arguments are equal to each other and {@code false} otherwise
	 * @see Object#equals(Object)
	 */
	public static boolean equals(List<Number> a, List<Number> b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == b) {
			return true;
		}
		if (a == null && b != null || a != null && b == null) {
			return false;
		}

		ListIterator<Number> e1 = a.listIterator();
		ListIterator<Number> e2 = b.listIterator();
		while (e1.hasNext() && e2.hasNext()) {
			Number o1 = e1.next();
			Number o2 = e2.next();
			if (!(o1 == null ? o2 == null : NumberUtils.compare(o1, o2) == 0))
				return false;
		}
		return !(e1.hasNext() || e2.hasNext());

	}

	/**
	 * Returns {@code true} if the numbers are equal to each other and {@code false} otherwise. Consequently, if both arguments are {@code null}, {@code true}
	 * is returned and if exactly one argument is {@code null}, {@code false} is returned. Otherwise, equality is determined by using the
	 * {@link NumberUtils#compare} method.
	 *
	 * @param a
	 *            First number
	 * @param b
	 *            Second number to be compared with {@code a} for equality
	 * @return {@code true} if the arguments are equal to each other and {@code false} otherwise
	 * @see Object#equals(Object)
	 */
	public static boolean equals(Number a, Number b) {
		return (a == null && b == null) || (a != null && NumberUtils.compare(a, b) == 0);
	}

	/**
	 * Compare both values in order to determine whether the graph value should be updated.
	 *
	 * @param restValue
	 *            Rest model string value
	 * @param graphValue
	 *            Graph string value
	 * @return true if restValue is not null and the restValue is not equal to the graph value. Otherwise false.
	 */
	public static <T> boolean shouldUpdate(T restValue, T graphValue) {
		return restValue != null && !restValue.equals(graphValue);
	}

}
