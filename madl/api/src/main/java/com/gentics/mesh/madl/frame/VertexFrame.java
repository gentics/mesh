package com.gentics.mesh.madl.frame;

import java.util.function.Function;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

public interface VertexFrame extends ElementFrame, com.syncleus.ferma.VertexFrame {

	/**
	 * Add a unique <b>out-bound</b> link to the given vertex for the given set of labels. Note that this method will effectively ensure that only one
	 * <b>out-bound</b> link exists between the two vertices for each label.
	 * 
	 * @param vertex
	 *            Target vertex
	 * @param labels
	 *            Labels to handle
	 */
	void setUniqueLinkOutTo(VertexFrame vertex, String... labels);

	void setUniqueLinkInTo(VertexFrame vertex, String... labels);

	void setSingleLinkOutTo(VertexFrame vertex, String... labels);

	void setSingleLinkInTo(VertexFrame vertex, String... labels);

	/**
	 * @deprecated Use {@link #out(String, Class)} instead.
	 */
	@Deprecated
	@Override
	VertexTraversal<?, ?, ?> out(String... labels);

	<T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz);

	<T extends ElementFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz);

	<T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz);

	<T extends ElementFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz);

	<T extends RawTraversalResult<?>> T traverse(final Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser);

}
