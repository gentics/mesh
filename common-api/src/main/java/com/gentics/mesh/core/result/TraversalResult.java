package com.gentics.mesh.core.result;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterators;

/**
 * MADL migration class for traversal results.
 *
 * @param <T>
 */
public class TraversalResult<T> implements Result<T> {

	private Iterable<T> it;

	private final static TraversalResult<?> EMPTY = new TraversalResult<>(Collections::emptyIterator);

	public TraversalResult() {
	}

	public TraversalResult(Iterable<? extends T> it) {
		this.it = (Iterable<T>) it;
	}

	public TraversalResult(Iterator<? extends T> it) {
		this.it = () -> (Iterator<T>) it;
	}

	public TraversalResult(Stream<? extends T> stream) {
		this.it = ((Stream<T>) stream)::iterator;
	}

	public long count() {
		return Iterators.size(it.iterator());
	}

	@Override
	public Iterable<T> iterable() {
		return it;
	}

	@Override
	public Iterator<T> iterator() {
		return it.iterator();
	}

	@Override
	public Stream<T> stream() {
		Stream<T> stream = StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(it.iterator(), Spliterator.ORDERED),
			false);
		return stream;
	}

	@Override
	public List<T> list() {
		return stream().collect(Collectors.toList());
	}

	@Override
	public boolean isEmpty() {
		return !hasNext();
	}

	@Override
	public boolean hasNext() {
		return iterator().hasNext();
	}

	@Override
	public T next() {
		return iterator().next();
	}

	@Override
	public T nextOrNull() {
		Iterator<T> iterator = iterator();
		if (iterator.hasNext()) {
			return iterator.next();
		} else {
			return null;
		}
	}

	public static <T> TraversalResult<T> empty() {
		return (TraversalResult<T>) EMPTY;
	}
}
