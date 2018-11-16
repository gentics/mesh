package com.gentics.mesh.madlmigration;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterators;

public class TraversalResult<T> {

	private Iterable<? extends T> it;

	public TraversalResult() {
	}

	public TraversalResult(Iterable<? extends T> it) {
		this.it = it;
	}

	public long count() {
		return Iterators.size(it.iterator());
	}

	public Iterable<? extends T> iterable() {
		return it;
	}

	public Iterator<? extends T> iterator() {
		return it.iterator();
	}

	public Stream<? extends T> stream() {
		Stream<T> stream = StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(it.iterator(), Spliterator.ORDERED),
			false);
		return stream;
	}

	public List<? extends T> list() {
		return stream().collect(Collectors.toList());
	}
}
