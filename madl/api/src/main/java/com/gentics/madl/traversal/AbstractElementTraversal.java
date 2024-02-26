package com.gentics.madl.traversal;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Element;

public abstract class AbstractElementTraversal<S, E extends Element> extends AbstractTraversal<S, E> implements Traversal<S, E> {

	public AbstractElementTraversal(GraphTraversal<S, E> traversal) {
		super(traversal);
	}

	/**
	 * Remove every element at the end of this Pipeline.
	 */
	public void removeAll() {
		rawTraversal().forEachRemaining(Element::remove);
	}
}
