package com.gentics.madl.traversal;

import java.util.function.Predicate;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.syncleus.ferma.FramedGraph;

public class VertexTraversalImpl<S, E extends Vertex> extends AbstractElementTraversal<S, E> implements VertexTraversal<S, E> {

	public VertexTraversalImpl(FramedGraph graph) {
		this(graph, (GraphTraversal) graph.getRawTraversal().V());
	}

	public VertexTraversalImpl(FramedGraph graph, GraphTraversal<S, E> traversal) {
		super(graph, traversal);
	}

	@Override
	public VertexTraversalImpl<S, E> filter(Predicate<E> predicate) {
		return new VertexTraversalImpl<>(graph, rawTraversal().filter(e -> predicate.test(e.get())));
	}
}
