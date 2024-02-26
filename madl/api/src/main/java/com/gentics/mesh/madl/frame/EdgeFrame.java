package com.gentics.mesh.madl.frame;

import org.apache.tinkerpop.gremlin.structure.Edge;

import com.gentics.madl.traversal.EdgeTraversal;
import com.gentics.madl.traversal.VertexTraversal;
import com.gentics.mesh.core.result.TraversalResult;

public interface EdgeFrame extends ElementFrame, com.syncleus.ferma.EdgeFrame {

	@Override
	Edge getElement();

	/**
	 * @return The in vertex for this edge.
	 */
	VertexTraversal<?, ?> inV();

	/**
	 * @return The out vertex of this edge.
	 */
	VertexTraversal<?, ?> outV();

	/**
	 * Shortcut to get Traversal of current element
	 *
	 * @return the EdgeTraversal of the current element
	 */
	EdgeTraversal<?, ?> traversal();

	/**
	 * Return the label of the edge.
	 * 
	 * @return
	 */
	default String label() {
		//TODO Move this to edge frame instead
		return getLabel();
	}

	<T extends VertexFrame> TraversalResult<? extends T> outV(Class<T> clazz);

	<T extends VertexFrame> TraversalResult<? extends T> inV(Class<T> clazz);
}
