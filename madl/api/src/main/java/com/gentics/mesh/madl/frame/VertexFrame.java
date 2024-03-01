package com.gentics.mesh.madl.frame;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.madl.traversal.EdgeTraversal;
import com.gentics.madl.traversal.VertexTraversal;
import com.gentics.madl.traversal.VertexTraversalImpl;
import com.gentics.mesh.core.result.Result;

public interface VertexFrame extends ElementFrame, com.syncleus.ferma.VertexFrame {

	@Override
	Vertex getElement();

	VertexTraversal<?, ?> out(final String... labels);

	VertexTraversal<?, ?> in(final String... labels);

	EdgeTraversal<?, ?> outE(final String... labels);

	EdgeTraversal<?, ?> inE(final String... labels);

	/**
	 * Create edges from the framed vertex to the supplied vertex with the supplied labels
	 *
	 * @param vertex
	 *            The vertex to link to.
	 * @param labels
	 *            The labels for the edges.
	 * @deprecated Use {@link VertexFrame#setLinkOut(com.syncleus.ferma.VertexFrame, String...)}
	 */
	@Deprecated
	default void linkOut(VertexFrame vertex, String... labels) {
		setLinkOut(vertex, labels);
	}

	/**
	 * Create edges from the supplied vertex to the framed vertex with the supplied labels
	 *
	 * @param vertex
	 *            The vertex to link from.
	 * @param labels
	 *            The labels for the edges.
	 * @deprecated Use {@link VertexFrame#setLinkIn(com.syncleus.ferma.VertexFrame, String...)}
	 */
	@Deprecated
	default void linkIn(VertexFrame vertex, String... labels) {
		setLinkIn(vertex, labels);
	}

	/**
	 * Shortcut to get frame Traversal of current element
	 *
	 * @return The traversal for the current element.
	 */
	default VertexTraversal<?, ?> traversal() {
		return new VertexTraversalImpl<>(getGraph());
	}

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

	<T extends ElementFrame> Result<? extends T> out(String label, Class<T> clazz);

	<T extends EdgeFrame> Result<? extends T> outE(String label, Class<T> clazz);

	<T extends ElementFrame> Result<? extends T> in(String label, Class<T> clazz);

	<T extends EdgeFrame> Result<? extends T> inE(String label, Class<T> clazz);
}
