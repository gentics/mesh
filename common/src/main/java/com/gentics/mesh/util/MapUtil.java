package com.gentics.mesh.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class MapUtil {
	private MapUtil() {
	}

	/**
	 * Merges all entries of the maps to a single map. On conflicts, entries from later maps take precedence.
	 * @param maps
	 * @param <K>
	 * @param <V>
	 * @return
	 */
	@SafeVarargs
	public static <K, V> Map<K, V> merge(Map<K, V> ...maps) {
		HashMap<K, V> result = new HashMap<>();
		Stream.of(maps).forEach(result::putAll);
		return result;
	}
}
