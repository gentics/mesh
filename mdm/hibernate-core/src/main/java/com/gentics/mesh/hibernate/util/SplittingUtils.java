package com.gentics.mesh.hibernate.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Lists;

/**
 * Contains methods used to split collections and perform actions on each split.
 */
public class SplittingUtils {

	/**
	 * Splits the collection in splitSize collections, and apply the provided function to each of those.
	 * Returns the result of each function merged into a single map of sets
	 * @param collection
	 * @param splitSize
	 * @param function
	 * @param <T>
	 * @param <K>
	 * @param <V>
	 * @return
	 */
	public static <T, K, V> Map<K, Set<V>> splitAndMergeInMapOfSets(Collection<T> collection, int splitSize, Function<Collection<T>, Map<K, Set<V>>> function) {
		Map<K, Set<V>> result = new HashMap<>();
		List<List<T>> lists = Lists.partition(new ArrayList<>(collection), splitSize);
		for (List<T> p : lists) {
			function.apply(p).entrySet().stream().forEach(entry -> {
				result.computeIfAbsent(entry.getKey(), unused -> new HashSet<>(entry.getValue().size())).addAll(entry.getValue());
			});
		}
		return result;
	}

	/**
	 * Splits the collection in splitSize collections, and apply the provided function to each of those.
	 * Returns the result of each function merged into a single map of lists
	 * @param collection
	 * @param splitSize
	 * @param function
	 * @param <T>
	 * @param <K>
	 * @param <V>
	 * @return
	 */
	public static <T, K, V> Map<K, List<V>> splitAndMergeInMapOfLists(Collection<T> collection, int splitSize, Function<Collection<T>, Map<K, List<V>>> function) {
		Map<K, List<V>> result = new HashMap<>();
		List<List<T>> lists = Lists.partition(new ArrayList<>(collection), splitSize);
		for (List<T> p : lists) {
			function.apply(p).entrySet().stream().forEach(entry -> {
				result.computeIfAbsent(entry.getKey(), unused -> new ArrayList<>(entry.getValue().size())).addAll(entry.getValue());
			});
		}
		return result;
	}

	/**
	 * Splits the collection in splitSize collections, and apply the provided function to each of those.
	 * Returns the result of each function merged into a single map
	 * @param collection
	 * @param splitSize
	 * @param function
	 * @param <T>
	 * @param <K>
	 * @param <V>
	 * @return
	 */
	public static <T, K, V> Map<K, V> splitAndMergeInMap(Collection<T> collection, int splitSize, Function<Collection<T>, Map<K, V>> function) {
		Map<K, V> result = new HashMap<>();
		List<List<T>> lists = Lists.partition(new ArrayList<>(collection), splitSize);
		for (List<T> p : lists) {
			Map<K, V> partialResult = function.apply(p);
			result.putAll(partialResult);
		}
		return result;
	}

	/**
	 * Splits the collection in splitSize collections, and apply the provided function to each of those.
	 * Returns the result of each function merged into a single list
	 * @param collection
	 * @param splitSize
	 * @param function
	 * @param <T>
	 * @param <V>
	 * @return
	 */
	public static <T, V> List<V> splitAndMergeInList(Collection<T> collection, int splitSize, Function<Collection<T>, List<V>> function) {
		List<V> result = new ArrayList<>();
		List<List<T>> lists = Lists.partition(new ArrayList<>(collection), splitSize);
		for (List<T> p : lists) {
			List<V> partialResult = function.apply(p);
			result.addAll(partialResult);
		}
		return result;
	}

	/**
	 * Splits the collection in splitSize collections, and apply the provided function to each of those.
	 * Returns the result of each function merged into a single set
	 * @param collection
	 * @param splitSize
	 * @param function
	 * @param <T>
	 * @param <V>
	 * @return
	 */
	public static <T, V> Set<V> splitAndMergeInSet(Collection<T> collection, int splitSize, Function<Collection<T>, Set<V>> function) {
		Set<V> result = new HashSet<>();
		List<List<T>> lists = Lists.partition(new ArrayList<>(collection), splitSize);
		for (List<T> p : lists) {
			Set<V> partialResult = function.apply(p);
			result.addAll(partialResult);
		}
		return result;
	}

	/**
	 * Splits the collection in splitSize collections, and call the consumer for each one of those.
	 * @param collection
	 * @param splitSize
	 * @param consumer
	 * @param <T>
	 * @return
	 */
	public static <T> void splitAndConsume(Collection<T> collection, int splitSize, Consumer<Collection<T>> consumer) {
		List<List<T>> lists = Lists.partition(new ArrayList<>(collection), splitSize);
		for (List<T> p : lists) {
			consumer.accept(p);
		}
	}

	/**
	 * Splits the collection in splitSize collections, and call the consumer for each one of those, counting the results.
	 * @param collection
	 * @param splitSize
	 * @param consumer
	 * @param <T>
	 * @return total result
	 */
	public static <T> long splitAndCount(Collection<T> collection, int splitSize, Function<Collection<T>, Long> consumer) {
		long count = 0;
		List<List<T>> lists = Lists.partition(new ArrayList<>(collection), splitSize);
		for (List<T> p : lists) {
			count += consumer.apply(p);
		}
		return count;
	}

	/**
	 * Splits the collection in splitSize collections, and call the consumer for each one of those, counting the progress.
	 * @param collection
	 * @param splitSize
	 * @param consumer
	 * @param <T>
	 * @return
	 */
	public static <T> void splitAndConsumeProgress(Collection<T> collection, int splitSize, BiConsumer<Collection<T>, Long> consumer) {
		long count = 0;
		List<List<T>> lists = Lists.partition(new ArrayList<>(collection), splitSize);
		for (List<T> p : lists) {
			count += p.size();
			consumer.accept(p, count);
		}
	}

	/**
	 * Splits the collection in splitSize collections, and call the consumer for each one of those with the count base, counting the results.
	 * @param collection
	 * @param splitSize
	 * @param consumer
	 * @param <T>
	 * @return total result
	 */
	public static <T> long splitAndCount(Collection<T> collection, int splitSize, BiFunction<Long, List<T>, Long> consumer) {
		long count = 0;
		List<List<T>> lists = Lists.partition(new ArrayList<>(collection), splitSize);
		for (List<T> p : lists) {
			count += consumer.apply(count, p);
		}
		return count;
	}
}
