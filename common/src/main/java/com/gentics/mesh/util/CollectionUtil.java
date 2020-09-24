package com.gentics.mesh.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
}
