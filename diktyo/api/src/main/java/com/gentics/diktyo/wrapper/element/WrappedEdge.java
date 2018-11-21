package com.gentics.diktyo.wrapper.element;

import org.apache.tinkerpop.gremlin.structure.Edge;

public interface WrappedEdge extends WrappedElement<Edge> {

	/**
	 * Return the label of the edge.
	 * 
	 * @return
	 */
	String label();

	/**
	 * Return the in bound vertex.
	 * 
	 * @param classOfR
	 * @return
	 */
	<R> R inV(Class<R> classOfR);

	/**
	 * Return the out bound vertex.
	 * 
	 * @param classOfR
	 * @return
	 */
	<R> R outV(Class<R> classOfR);

}
