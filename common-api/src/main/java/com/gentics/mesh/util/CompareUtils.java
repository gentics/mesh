package com.gentics.mesh.util;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Utils used for comparing objects.
 */
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
	 * Default lists comparator.
	 * 
	 * @param <T>
	 * @param a
	 * @param b
	 * @return
	 * @see {@link CompareUtils#equals(List, List, Optional)}
	 */
	public static <T> boolean equals(List<? extends T> a, List<? extends T> b) {
		return equals(a, b, Optional.empty());
	}

	/**
	 * Returns {@code true} if the lists are equal to each other and {@code false} otherwise. Consequently, if both arguments are {@code null},
	 * {@code true} is returned and if exactly one argument is {@code null}, {@code false} is returned. Otherwise, equality is determined by using either the
	 * {@link Objects#equals(Object, Object)} or custom comparator method by comparing each list item.
	 *
	 * @param a
	 *            First list
	 * @param b
	 *            Second list to be compared with {@code a} for equality
	 * @param comparator
	 * 			  Optional custom item comparator
	 * @return {@code true} if the arguments are equal to each other and {@code false} otherwise
	 */
	public static <T> boolean equals(List<? extends T> a, List<? extends T> b, Optional<BiFunction<T, T, Boolean>> comparator) {
		if (a == null && b == null) {
			return true;
		}
		if (a == b) {
			return true;
		}
		if (a == null && b != null || a != null && b == null) {
			return false;
		}

		ListIterator<? extends T> e1 = a.listIterator();
		ListIterator<? extends T> e2 = b.listIterator();
		while (e1.hasNext() && e2.hasNext()) {
			T o1 = e1.next();
			T o2 = e2.next();
			if (!(o1 == null ? o2 == null : comparator.orElse(Objects::equals).apply(o1, o2)))
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
