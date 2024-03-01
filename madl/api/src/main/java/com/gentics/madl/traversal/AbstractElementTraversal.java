package com.gentics.madl.traversal;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Element;

import com.syncleus.ferma.FramedGraph;

public abstract class AbstractElementTraversal<S, E extends Element> extends AbstractTraversal<S, E> implements Traversal<S, E> {

	protected final FramedGraph graph;

	public AbstractElementTraversal(FramedGraph graph, GraphTraversal<S, E> traversal) {
		super(traversal);
		this.graph = graph;
	}

	/**
	 * Remove every element at the end of this Pipeline.
	 */
	public void removeAll() {
		rawTraversal().forEachRemaining(Element::remove);
	}

	/**
	 * Get the whole graph.
	 * 
	 * @return
	 */
	public FramedGraph getGraph() {
		return graph;
	}
}
