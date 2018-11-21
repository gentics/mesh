package com.gentics.diktyo.wrapper.element;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.diktyo.wrapper.traversal.WrappedTraversal;

public interface WrappedVertex extends WrappedElement<Vertex> {

	// traverse()
	
	WrappedTraversal<Vertex> out(String label);

	WrappedTraversal<Vertex> in(String label);

	WrappedTraversal<Edge> outE(String label);

	WrappedTraversal<Edge> inE(String label);

	void linkOut(WrappedVertex v, String label);

	void linkIn(WrappedVertex v, String label);

	void unlinkOut(WrappedVertex v, String label);

	void unlinkIn(WrappedVertex v, String label);

	void setLinkOut(WrappedVertex v, String label);

	void setLinkIn(WrappedVertex v, String label);

}
