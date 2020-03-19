/**
 * Copyright 2004 - 2016 Syncleus, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.syncleus.ferma;

import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public interface VertexFrame extends ElementFrame {
	@Override
	Vertex getElement();

	/**
	 * Add an edge using the supplied frame type.
	 *
	 * @param <T>
	 *            The type for the framed edge.
	 * @param label
	 *            The label for the edge
	 * @param inVertex
	 *            The vertex to link to.
	 * @param initializer
	 *            the initializer for the frame which defines its type and may initialize properties
	 * @return The new edge.
	 */
	<T> T addFramedEdge(String label, VertexFrame inVertex, ClassInitializer<T> initializer);

	/**
	 * Add an edge using the supplied frame type.
	 *
	 * @param <T>
	 *            The type for the framed edge.
	 * @param label
	 *            The label for the edge
	 * @param inVertex
	 *            The vertex to link to.
	 * @param kind
	 *            The kind of frame.
	 * @return The new edge.
	 */
	<T> T addFramedEdge(String label, VertexFrame inVertex, Class<T> kind);

	/**
	 * Add an edge using the supplied frame type.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <T>
	 *            The type for the framed edge.
	 * @param label
	 *            The label for the edge
	 * @param inVertex
	 *            The vertex to link to.
	 * @param initializer
	 *            the initializer for the frame which defines its type and may initialize properties
	 * @return The new edge.
	 */
	<T> T addFramedEdgeExplicit(String label, VertexFrame inVertex, ClassInitializer<T> initializer);

	/**
	 * Add an edge using the supplied frame type.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <T>
	 *            The type for the framed edge.
	 * @param label
	 *            The label for the edge
	 * @param inVertex
	 *            The vertex to link to.
	 * @param kind
	 *            The kind of frame.
	 * @return The new edge.
	 */
	<T> T addFramedEdgeExplicit(String label, VertexFrame inVertex, Class<T> kind);

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

	/**
	 * @deprecated Use out(label, clazz) instead.
	 * @param labels
	 * @return
	 */
	@Deprecated
	VertexTraversal<?, ?, ?> out(final String... labels);

	VertexTraversal<?, ?, ?> in(final String... labels);

	EdgeTraversal<?, ?, ?> outE(final String... labels);

	EdgeTraversal<?, ?, ?> inE(final String... labels);

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
	VertexTraversal<?, ?, ?> traversal();

	/**
	 * Output the vertex as JSON.
	 *
	 * @return A JsonObject representing this frame.
	 */
	JsonObject toJson();

	/**
	 * Reframe this element as a different type of frame.
	 *
	 * @param <T>
	 *            The type used to frame the element.
	 * @param kind
	 *            The new kind of frame.
	 * @return The new frame
	 */
	<T> T reframe(Class<T> kind);

	/**
	 * Reframe this element as a different type of frame.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <T>
	 *            The type used to frame the element.
	 * @param kind
	 *            The new kind of frame.
	 * @return The new frame
	 */
	<T> T reframeExplicit(Class<T> kind);

}
