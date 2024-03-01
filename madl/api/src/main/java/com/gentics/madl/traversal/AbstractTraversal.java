package com.gentics.madl.traversal;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;

public abstract class AbstractTraversal<S, E> implements Traversal<S, E> {

	protected final GraphTraversal<S, E> traversal;

	public AbstractTraversal(GraphTraversal<S, E> traversal) {
		this.traversal = traversal;
	}

	@Override
	public GraphTraversal<S, E> rawTraversal() {
		return traversal;
	}

	@Override
	public void close() throws Exception {
		traversal.close();
	}
}
