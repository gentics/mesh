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

import com.syncleus.ferma.traversals.VertexTraversal;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.*;

import java.util.Iterator;

/**
 * The primary class for framing your blueprints graphs.
 */
public interface FramedGraph extends Graph {

	TypeResolver getTypeResolver();

	/**
	 * Close the delegate graph.
	 */
	void close();

	/**
	 * Add a vertex to the graph
	 *
	 * @param <T>
	 *            The type used to frame the element.
	 * @param id
	 *            the recommended object identifier
	 * @param initializer
	 *            the initializer for the frame which defines its type and may initialize properties
	 * @return The framed vertex.
	 */
	<T> T addFramedVertex(Object id, ClassInitializer<T> initializer);

	/**
	 * Add a vertex to the graph
	 *
	 * @param <T>
	 *            The type used to frame the element.
	 * @param kind
	 *            The kind of the frame.
	 * @return The framed vertex.
	 */
	<T> T addFramedVertex(Class<T> kind);

	/**
	 * Add a vertex to the graph
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <T>
	 *            The type used to frame the element.
	 * @param initializer
	 *            the initializer for the frame which defines its type and may initialize properties
	 * @return The framed vertex.
	 */
	<T> T addFramedVertexExplicit(ClassInitializer<T> initializer);

	/**
	 * Add a vertex to the graph
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <T>
	 *            The type used to frame the element.
	 * @param kind
	 *            The kind of the frame.
	 * @return The framed vertex.
	 */
	<T> T addFramedVertexExplicit(Class<T> kind);

	/**
	 * Add a vertex to the graph using a frame type of {@link TVertex}.
	 *
	 * @return The framed vertex.
	 */
	TVertex addFramedVertex();

	/**
	 * Add a edge to the graph
	 *
	 * @param <T>
	 *            The type used to frame the element.
	 * @param id
	 *            the recommended object identifier
	 * @param source
	 *            the source vertex
	 * @param destination
	 *            the destination vertex
	 * @param label
	 *            the label.
	 * @param initializer
	 *            the initializer for the frame which defines its type and may initialize properties
	 * @return The framed edge.
	 */
	<T> T addFramedEdge(final Object id, final VertexFrame source, final VertexFrame destination, final String label,
		ClassInitializer<T> initializer);

	/**
	 * Add a edge to the graph
	 *
	 * @param <T>
	 *            The type used to frame the element.
	 * @param source
	 *            The source vertex
	 * @param destination
	 *            the destination vertex
	 * @param label
	 *            the label.
	 * @param kind
	 *            The kind of the frame.
	 * @return The framed edge.
	 */
	<T> T addFramedEdge(final VertexFrame source, final VertexFrame destination, final String label, Class<T> kind);

	/**
	 * Add a edge to the graph using a frame type of {@link TEdge}.
	 *
	 * @param source
	 *            The source vertex
	 * @param destination
	 *            the destination vertex
	 * @param label
	 *            the label.
	 * @return The framed edge.
	 */
	TEdge addFramedEdge(final VertexFrame source, final VertexFrame destination, final String label);

	/**
	 * Query over all vertices in the graph.
	 *
	 * @return The query.
	 */
	VertexTraversal<?, ?, ?> v();

	/**
	 * Query over all edges in the graph.
	 *
	 * @return The query.
	 */
	EdgeTraversal<?, ?, ?> e();

	<F> F getFramedVertexExplicit(Class<F> classOfF, Object id);

	<F> Iterable<? extends F> getFramedVertices(final String key, final Object value, final Class<F> kind);

	<F> Iterable<? extends F> getFramedVerticesExplicit(final Class<F> kind);

	<F> Iterable<? extends F> getFramedVerticesExplicit(final String key, final Object value, final Class<F> kind);

	<F> Iterable<? extends F> getFramedEdges(final Class<F> kind);

	<F> Iterable<? extends F> getFramedEdges(final String key, final Object value, final Class<F> kind);

	<F> Iterable<? extends F> getFramedEdgesExplicit(final String key, final Object value, final Class<F> kind);

	/**
	 * Query over a list of edges in the graph.
	 *
	 * @param ids
	 *            The ids of the edges.
	 * @return The query.
	 */
	EdgeTraversal<?, ?, ?> e(final Object... ids);

	Vertex addVertexExplicit(Object id);

	Edge addEdgeExplicit(Object id, Vertex outVertex, Vertex inVertex, String label);

	<T> Iterator<? extends T> frame(Iterator<? extends Element> pipeline, final Class<T> kind);

	<T> T frameNewElement(Element e, ClassInitializer<T> initializer);

	<T> T frameNewElement(Element e, Class<T> kind);

	<T> T frameElement(Element e, Class<T> kind);

	<T> T frameNewElementExplicit(Element e, ClassInitializer<T> initializer);

	<T> T frameNewElementExplicit(Element e, Class<T> kind);

	<T> T frameElementExplicit(Element e, Class<T> kind);

	<T> Iterator<? extends T> frameExplicit(Iterator<? extends Element> pipeline, final Class<T> kind);

	<T> T frameElementExplicitById(final Object id, final Class<T> kind);
}
