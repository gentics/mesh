package com.gentics.diktyo.orientdb3.wrapper.element;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.diktyo.orientdb3.wrapper.traversal.WrappedTraversalImpl;
import com.gentics.diktyo.wrapper.element.AbstractWrappedCoreVertex;
import com.gentics.diktyo.wrapper.traversal.WrappedTraversal;

public class AbstractWrappedVertex extends AbstractWrappedCoreVertex {

	@Override
	public void init(Vertex element) {
		setDelegate(element);
	}

	@Override
	public WrappedTraversal<Vertex> out(String label) {
		Iterator<Vertex> it = delegate().vertices(Direction.OUT, label);
		return new WrappedTraversalImpl<Vertex>(it);
	}

	@Override
	public WrappedTraversal<Vertex> in(String label) {
		Iterator<Vertex> it = delegate().vertices(Direction.IN, label);
		return new WrappedTraversalImpl<Vertex>(it);
	}

	@Override
	public WrappedTraversal<Edge> outE(String label) {
		Iterator<Edge> it = delegate().edges(Direction.OUT, label);
		return new WrappedTraversalImpl<Edge>(it);
	}

	@Override
	public WrappedTraversal<Edge> inE(String label) {
		Iterator<Edge> it = delegate().edges(Direction.IN, label);
		return new WrappedTraversalImpl<Edge>(it);
	}

}
