package com.gentics.madl.traversal;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterators;

public interface BaseTraversalResult<T> extends Iterable<T> {

	/**
	 * Count the amount of results.
	 * 
	 * @return
	 */
	default long count() {
		return Iterators.size(iterator());
	}

	/**
	 * Transform the result into a stream.
	 * 
	 * @return
	 */
	default Stream<? extends T> stream() {
		Stream<T> stream = StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED),
			false);
		return stream;
	}

	/**
	 * Transform the result into a list. Be aware that this operation may be memory intensive if the result is large.
	 * 
	 * @return
	 */
	default List<? extends T> list() {
		return stream().collect(Collectors.toList());
	}

	/**
	 * Check whether the traversal result is empty.
	 * 
	 * @return
	 */
	default boolean isEmpty() {
		return !hasNext();
	}

	/**
	 * Check whether the traversal result has another result.
	 * 
	 * @return
	 */
	default boolean hasNext() {
		return iterator().hasNext();
	}

	/**
	 * Return the first element of the traversal.
	 * 
	 * @return
	 */
	default T first() {
		return iterator().next();
	}

	default T firstOrNull() {
		if (iterator().hasNext()) {
			return iterator().next();
		} else {
			return null;
		}
	}
}
