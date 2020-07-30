package com.gentics.mesh.madl.traversal;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterators;

/**
 * MADL migration class for traversal results.
 *
 * @param <T>
 */
public class TraversalResult<T> implements Iterable<T> {

	private Iterable<T> it;

	public TraversalResult() {
	}

	public TraversalResult(Iterable<T> it) {
		this.it = it;
	}

	public TraversalResult(Iterator<T> it) {
		this.it = () -> it;
	}

	public TraversalResult(Stream<T> stream) {
		this.it = stream::iterator;
	}

	public long count() {
		return Iterators.size(it.iterator());
	}

	public Iterator<T> iterator() {
		return it.iterator();
	}

	public Stream<? extends T> stream() {
		Stream<T> stream = StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(it.iterator(), Spliterator.ORDERED),
			false);
		return stream;
	}

	public <R> TraversalResult<R> map(Function<? super T, ? extends R> mapper) {
		return new TraversalResult<>(stream().map(mapper));
	}

	public List<? extends T> list() {
		return stream().collect(Collectors.toList());
	}

	/**
	 * Check whether the traversal result is empty.
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return !hasNext();
	}

	public boolean hasNext() {
		return iterator().hasNext();
	}

	public T next() {
		return iterator().next();
	}

	public T nextOrNull() {
		Iterator<T> iterator = iterator();
		if (iterator.hasNext()) {
			return iterator.next();
		} else {
			return null;
		}
	}
}
