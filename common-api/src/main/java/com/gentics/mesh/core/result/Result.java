package com.gentics.mesh.core.result;

import java.util.List;
import java.util.stream.Stream;

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

	List<T> list();

	Stream<T> stream();

	Iterable<T> iterable();

	long count();
}
