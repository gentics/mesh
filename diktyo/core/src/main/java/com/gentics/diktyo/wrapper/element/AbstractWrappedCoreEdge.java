package com.gentics.diktyo.wrapper.element;

import org.apache.tinkerpop.gremlin.structure.Edge;

public abstract class AbstractWrappedCoreEdge extends AbstractWrappedCoreElement<Edge> implements WrappedEdge {

	@Override
	public void init(Edge element) {
		setDelegate(element);
	}

	@Override
	public String label() {
		return delegate().label();
	}

}
