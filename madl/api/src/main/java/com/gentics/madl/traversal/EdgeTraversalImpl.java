package com.gentics.madl.traversal;

import java.util.function.Predicate;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;

public class EdgeTraversalImpl<S, E extends Edge> extends AbstractElementTraversal<S, E> implements EdgeTraversal<S, E> {

	public EdgeTraversalImpl(GraphTraversal<S, E> traversal) {
		super(traversal);
	}

	@Override
	public EdgeTraversalImpl<S, E> filter(Predicate<E> predicate) {
		return new EdgeTraversalImpl<>(rawTraversal().filter(e -> predicate.test(e.get())));
	}
}
