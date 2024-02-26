package com.gentics.mesh.madl.frame;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.madl.traversal.EdgeTraversal;
import com.gentics.madl.traversal.VertexTraversal;
import com.gentics.mesh.core.result.Result;
import com.syncleus.ferma.TEdge;

public interface VertexFrame extends ElementFrame, com.syncleus.ferma.VertexFrame {

	@Override
	Vertex getElement();

	/**
	 * Add an edge using a frame type of {@link TEdge}.
	 *
	 * @param label
	 *            The label for the edge
	 * @param inVertex
	 *            The vertex to link to.
	 * @return The added edge.
	 */
	TEdge addFramedEdge(String label, VertexFrame inVertex);

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
	 */
	void linkOut(VertexFrame vertex, String... labels);

	/**
	 * Create edges from the supplied vertex to the framed vertex with the supplied labels
	 *
	 * @param vertex
	 *            The vertex to link from.
	 * @param labels
	 *            The labels for the edges.
	 */
	void linkIn(VertexFrame vertex, String... labels);

	/**
	 * Remove all out edges to the supplied vertex with the supplied labels.
	 *
	 * @param vertex
	 *            The vertex to removed the edges to.
	 * @param labels
	 *            The labels of the edges.
	 */
	void unlinkOut(VertexFrame vertex, String... labels);

	/**
	 * Remove all in edges to the supplied vertex with the supplied labels.
	 *
	 * @param vertex
	 *            The vertex to removed the edges from.
	 * @param labels
	 *            The labels of the edges.
	 */
	void unlinkIn(VertexFrame vertex, String... labels);

	/**
	 * Remove all out edges with the labels and then add a single edge to the supplied vertex.
	 *
	 * @param vertex
	 *            the vertex to link to.
	 * @param labels
	 *            The labels of the edges.
	 */
	void setLinkOut(VertexFrame vertex, String... labels);

	/**
	 * Shortcut to get frame Traversal of current element
	 *
	 * @return The traversal for the current element.
	 */
	VertexTraversal<?, ?> traversal();

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
