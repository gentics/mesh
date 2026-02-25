package com.gentics.mesh.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;

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
		return equals(a, b, false, false);
	}

	/**
	 * Returns {@code true} if the arguments are equal to each other and {@code false} otherwise. Consequently, if both arguments are {@code null}, {@code true}
	 * is returned and if exactly one argument is {@code null}, {@code false} is returned. Otherwise, equality is determined by using the {@link Object#equals
	 * equals} method of the first argument.
	 *
	 * @param a
	 *            an object
	 * @param b
	 *            an object to be compared with {@code a} for equality
	 * @param nullIsEmpty true if null value is logically equal to an empty value
	 * @param nullIsUnchanged true if null value for objectB shall be treated as "unchanged"
	 * @return {@code true} if the arguments are equal to each other and {@code false} otherwise
	 * @see Object#equals(Object)
	 */
	public static boolean equals(Object a, Object b, boolean nullIsEmpty, boolean nullIsUnchanged) {
		if (nullIsUnchanged && b == null) {
			return true;
		}
		if (nullIsEmpty && (
				(b == null && isEmpty(a)) ||
				(a == null && isEmpty(b))
			)) {
			return true;
		}
		if (a instanceof Object[] arrayA && b instanceof Object[] arrayB) {
			return Arrays.equals(arrayA, arrayB);
		} else {
			return (a == null && b == null) || (a != null && a.equals(b));
		}
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
	 * Check whether the given object is considered empty:
	 * <ol>
	 * <li>null</li>
	 * <li>empty string</li>
	 * <li>empty collection</li>
	 * <li>empty array</li>
	 * </ol>
	 * @param o object to check
	 * @return true if the object is empty
	 */
	public static boolean isEmpty(Object o) {
		if (o == null) {
			return true;
		} else if (o instanceof String s) {
			return StringUtils.isEmpty(s);
		} else if (o instanceof Collection<?> c) {
			return c.isEmpty();
		} else if (o instanceof Map<?, ?> m) {
			return m.isEmpty();
		} else if (o instanceof Object[] array) {
			return array.length == 0;
		} else {
			return false;
		}
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
