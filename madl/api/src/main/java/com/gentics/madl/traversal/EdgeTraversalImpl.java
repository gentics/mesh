package com.gentics.madl.traversal;

import java.util.function.Predicate;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;

import com.syncleus.ferma.FramedGraph;

public class EdgeTraversalImpl<S, E extends Edge> extends AbstractElementTraversal<S, E> implements EdgeTraversal<S, E> {

	public EdgeTraversalImpl(FramedGraph graph) {
		this(graph, (GraphTraversal) graph.getRawTraversal().V());
	}

	public EdgeTraversalImpl(FramedGraph graph, GraphTraversal<S, E> traversal) {
		super(graph, traversal);
	}

	@Override
	public EdgeTraversalImpl<S, E> filter(Predicate<E> predicate) {
		return new EdgeTraversalImpl<>(graph, rawTraversal().filter(e -> predicate.test(e.get())));
	}
}
