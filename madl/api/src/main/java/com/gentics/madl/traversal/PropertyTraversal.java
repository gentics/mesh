package com.gentics.madl.traversal;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;

import com.gentics.mesh.util.StreamUtil;

public class PropertyTraversal<S, E> extends AbstractTraversal<S, E> implements Traversal<S, E> {

	public PropertyTraversal(GraphTraversal<S, E> traversal) {
		super(traversal);
	}

	@Override
	public Traversal<S, E> filter(Predicate<E> predicate) {
		return new PropertyTraversal<>(rawTraversal().filter(e -> predicate.test(e.get())));
	}

	@Override
	public Traversal<S, E> retain(Iterable<?> collection) {
		return new PropertyTraversal<>(rawTraversal().is(P.within(StreamUtil.toStream(collection).collect(Collectors.toList()).toArray())));
	}
}
