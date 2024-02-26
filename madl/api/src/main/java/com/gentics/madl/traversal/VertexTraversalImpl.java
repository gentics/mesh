package com.gentics.madl.traversal;

import java.util.function.Predicate;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class VertexTraversalImpl<S, E extends Vertex> extends AbstractElementTraversal<S, E> implements VertexTraversal<S, E> {

	public VertexTraversalImpl(GraphTraversal<S, E> traversal) {
		super(traversal);
	}

	@Override
	public VertexTraversalImpl<S, E> filter(Predicate<E> predicate) {
		return new VertexTraversalImpl<>(rawTraversal().filter(e -> predicate.test(e.get())));
	}
}
