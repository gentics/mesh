package com.gentics.mesh.madl.frame;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;

import com.gentics.mesh.core.result.TraversalResult;

public interface EdgeFrame extends ElementFrame, com.syncleus.ferma.EdgeFrame {

	/**
	 * Return the label of the edge.
	 * 
	 * @return
	 */
	default String label() {
		//TODO Move this to edge frame instead
		return getLabel();
	}

	/**
	 * Shortcut to get Traversal of current element
	 *
	 * @return the EdgeTraversal of the current element
	 */
	GraphTraversal<?, ?> traversal();

	<T extends VertexFrame> TraversalResult<? extends T> outV(Class<T> clazz);

	<T extends VertexFrame> TraversalResult<? extends T> inV(Class<T> clazz);
}
