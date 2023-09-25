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
/*
 * Part or all of this source file was forked from a third-party project, the details of which are listed below.
 *
 * Source Project: Totorom
 * Source URL: https://github.com/BrynCooke/totorom
 * Source License: Apache Public License v2.0
 * When: November, 20th 2014
 */
package com.syncleus.ferma.traversals;

import java.util.List;

import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.VertexFrame;
import com.tinkerpop.gremlin.Tokens;

/**
 * Vertex specific traversal.
 *
 * @param <C>
 *            The cap of the current pipe.
 * @param <S>
 *            The SideEffect of the current pipe.
 * @param <M>
 *            The current marked type for the current pipe.
 */
@Deprecated
public interface VertexTraversal<C, S, M> extends Traversal<VertexFrame, C, S, M> {

	/**
	 * Check if the element has a property with provided key.
	 *
	 * @param key
	 *            the property key to check
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> has(String key);

	/**
	 * If the incoming element has the provided key/value as check with .equals(), then let the element pass. If the key is id or label, then use respect id or
	 * label filtering.
	 *
	 * @param key
	 *            the property key to check.
	 * @param value
	 *            the object to filter on (in an OR manner)
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> has(String key, Object value);

	/**
	 * If the incoming element has the provided key/value as check with .equals(), then let the element pass. If the key is id or label, then use respect id or
	 * label filtering.
	 *
	 * @param key
	 *            the property key to check
	 * @param compareToken
	 *            the comparison to use
	 * @param value
	 *            the object to filter on
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> has(String key, Tokens.T compareToken, Object value);

	/**
	 * If the incoming vertex has the provided ferma_type property that is checked against the given class, then let the element pass.
	 *
	 * @param clazz
	 *            the class to check against
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> has(Class<?> clazz);

	/**
	 * If the incoming element has the provided key/value as check with .equals(), then filter the element. If the key is id or label, then use respect id or
	 * label filtering.
	 *
	 * @param key
	 *            the property key to check
	 * @param value
	 *            the objects to filter on (in an OR manner)
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> hasNot(String key, Object value);

	/**
	 * If the incoming element has the provided key filled, then filter the element. If the key is id or label, then use respect id or label filtering.
	 *
	 * @param key
	 *            the property key to check
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> hasNot(String key);

	/**
	 * Emit the adjacent outgoing vertices of the incoming vertex.
	 *
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> out(String... labels);

	/**
	 * Emit the adjacent incoming vertices for the incoming vertex.
	 *
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> in(String... labels);

	/**
	 * Emit the outgoing edges for the incoming vertex.
	 *
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> outE(String... labels);

	/**
	 * Emit the incoming edges for the incoming vertex.
	 *
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> inE(String... labels);

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a NoSuchElementException is thrown.
	 * 
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The type of frame for the element.
	 * @return the next emitted object
	 */
	<N> N next(Class<N> kind);

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a NoSuchElementException is thrown.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The type of frame for the element.
	 * @return the next emitted object
	 */
	<N> N nextExplicit(Class<N> kind);

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a the default value is returned.
	 * 
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The type of frame for the element.
	 * @param defaultValue
	 *            The object to return if no next object exists.
	 * @return the next emitted object
	 */
	<N> N nextOrDefault(Class<N> kind, N defaultValue);

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a the default value is returned.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The type of frame for the element.
	 * @param defaultValue
	 *            The object to return if no next object exists.
	 * @return the next emitted object
	 */
	<N> N nextOrDefaultExplicit(Class<N> kind, N defaultValue);

	/**
	 * Return an iterator of framed elements.
	 * 
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return An iterator of framed elements.
	 */
	<N> Iterable<N> frame(Class<N> kind);

	/**
	 * Return an iterator of framed elements.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return An iterator of framed elements.
	 */
	<N> Iterable<N> frameExplicit(Class<N> kind);

	/**
	 * Emit the incoming vertex, but have other vertex provide an outgoing edge to incoming vertex.
	 *
	 * @param label
	 *            the edge label
	 * @param other
	 *            the other vertex
	 * @return the extended Pipeline
	 */
	VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkOut(String label, VertexFrame other);

	/**
	 * Emit the incoming vertex, but have other vertex provide an incoming edge to incoming vertex.
	 *
	 * @param label
	 *            the edge label
	 * @param other
	 *            the other vertex
	 * @return the extended Pipeline
	 */
	VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkIn(String label, VertexFrame other);

	@Override
	VertexTraversal<?, ?, M> filter(TraversalFunction<VertexFrame, Boolean> filterFunction);

	/**
	 * Will emit the object only if it is in the provided collection.
	 *
	 * @param vertices
	 *            the collection to retain
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> retain(VertexFrame... vertices);

	@Override
	VertexTraversal<?, ?, M> retain(Iterable<?> collection);

	@Override
	VertexTraversal<C, S, ? extends VertexTraversal<C, S, M>> mark();

	/**
	 * Remove every element at the end of this Pipeline.
	 */
	void removeAll();

}
