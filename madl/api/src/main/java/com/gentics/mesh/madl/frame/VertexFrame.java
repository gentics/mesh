package com.gentics.mesh.madl.frame;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;

import com.gentics.mesh.core.result.Result;

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

	<T extends ElementFrame> GraphTraversal<?, ?> traversalOut(Class<T> clazz, String... labels);

	<T extends ElementFrame> GraphTraversal<?, ?> traversalIn(Class<T> clazz, String... labels);

	<T extends ElementFrame> Result<? extends T> out(String label, Class<T> clazz);

	<T extends EdgeFrame> Result<? extends T> outE(String label, Class<T> clazz);

	<T extends ElementFrame> Result<? extends T> in(String label, Class<T> clazz);

	<T extends EdgeFrame> Result<? extends T> inE(String label, Class<T> clazz);
}
