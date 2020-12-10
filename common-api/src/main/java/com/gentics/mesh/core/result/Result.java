package com.gentics.mesh.core.result;

import java.util.List;
import java.util.stream.Stream;

/**
 * Result of a database load operation.
 *
 * @param <T>
 */
public interface Result<T> extends Iterable<T> {

	/**
	 * Check whether the traversal result is empty.
	 * 
	 * @return
	 */
	boolean isEmpty();

	T nextOrNull();

	T next();

	boolean hasNext();

	/**
	 * Return the result as a list. Please note that the whole data needs to be loaded. This method should thus only be used for smaller datasets.
	 * 
	 * @return
	 */
	List<T> list();

	/**
	 * Return a stream of elements.
	 * 
	 * @return
	 */
	Stream<T> stream();

	/**
	 * Return the iterable for the result.
	 * 
	 * @return
	 */
	Iterable<T> iterable();

	/**
	 * Count the results. Please note that this will seek the datastream and calls to {@link #next()} or {@link #nextOrNull()} or {@link #stream()} will yield no element/s.
	 * @return
	 */
	long count();
}
