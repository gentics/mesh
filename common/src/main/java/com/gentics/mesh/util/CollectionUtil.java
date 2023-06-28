package com.gentics.mesh.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Utility which provides additional collection methods which are not present in JDK 8
 */
public final class CollectionUtil {

	private CollectionUtil() {
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

	/**
	 * Polls max(count, queue.size()) elements from the queue.
	 *
	 * @param queue
	 * @param count
	 * @param <T>
	 * @return
	 */
	public static <T> List<T> pollMany(Queue<T> queue, int count) {
		ArrayList<T> result = new ArrayList<>();
		int currentCount = 0;
		while (currentCount < count && !queue.isEmpty()) {
			T polled = queue.poll();
			result.add(polled);
			currentCount++;
		}

		return result;
	}
}
