package com.gentics.diktyo.wrapper.element;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public abstract class AbstractWrappedCoreVertex extends AbstractWrappedCoreElement<Vertex> implements WrappedVertex {

	@Override
	public Object id() {
		return delegate().id();
	}

	@Override
	public void remove() {
		delegate().remove();
	}

	@Override
	public void linkOut(WrappedVertex v, String label) {
		delegate().addEdge(label, v.delegate());
	}

	@Override
	public void linkIn(WrappedVertex v, String label) {
		v.linkOut(this, label);
	}

	@Override
	public void unlinkOut(WrappedVertex v, String label) {
		delegate().edges(Direction.OUT, label);
	}

	@Override
	public void unlinkIn(WrappedVertex v, String label) {
		delegate().edges(Direction.IN, label);
	}

	@Override
	public void setLinkOut(WrappedVertex v, String label) {
		delegate().addEdge(label, v.delegate());
	}

	@Override
	public void setLinkIn(WrappedVertex v, String label) {
		v.delegate().addEdge(label, delegate());
	}

}
