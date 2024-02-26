package com.gentics.madl.traversal;

import java.util.function.Predicate;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;

public class PropertyTraversal<S, E> extends AbstractTraversal<S, E> implements Traversal<S, E> {

	public PropertyTraversal(GraphTraversal<S, E> traversal) {
		super(traversal);
	}

	@Override
	public Traversal<S, E> filter(Predicate<E> predicate) {
		return new PropertyTraversal<>(rawTraversal().filter(e -> predicate.test(e.get())));
	}
}
