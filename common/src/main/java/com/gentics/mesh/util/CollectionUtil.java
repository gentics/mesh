package com.gentics.mesh.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility which provides additional collection methods which are not present in JDK 8
 */
public final class CollectionUtil {

	private CollectionUtil() {
	}

	/**
	 * Creates an unmodifiable set containing the given values.
	 *
	 * @param <T>
	 * @return
	 * @deprecated Use Java 9 Set.of(...) instead.
	 */
	@Deprecated
	public static <T> Set<T> setOf(T... elements) {
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(elements)));
	}

	/**
	 * For each provided elements, check that the map contains it as a key. If not contained, it will add it to the map
	 * with its default value.
	 * @param map
	 * @param elements
	 * @param fallbackValue
	 * @param <T>
	 * @param <R>
	 * @return
	 */
	public static <T, R> Map<T, R> addFallbackValueForMissingKeys(Map<T, R> map, Collection<T> elements, R fallbackValue) {
		for (T element : elements) {
			if (!map.containsKey(element)) {
				map.put(element, fallbackValue);
			}
		}

		return map;
	}
}
