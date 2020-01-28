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
import com.tinkerpop.blueprints.Predicate;
import com.tinkerpop.gremlin.Tokens;

/**
 * Edge specific traversal.
 *
 * @param <C>
 *            The cap of the current pipe.
 * @param <S>
 *            The SideEffect of the current pipe.
 * @param <M>
 *            The current mark'ed type for the current pipe.
 */
@Deprecated
public interface EdgeTraversal<C, S, M> extends Traversal<EdgeFrame, C, S, M> {

	/**
	 * Check if the element has a property with provided key.
	 *
	 * @param key
	 *            the property key to check
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> has(String key);

	/**
	 * Add an IdFilterPipe, LabelFilterPipe, or PropertyFilterPipe to the end of the Pipeline. If the incoming element has the provided key/value as check with
	 * .equals(), then let the element pass. If the key is id or label, then use respect id or label filtering.
	 *
	 * @param key
	 *            the property key to check
	 * @param value
	 *            the object to filter on (in an OR manner)
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> has(String key, Object value);

	/**
	 * Add an IdFilterPipe, LabelFilterPipe, or PropertyFilterPipe to the end of the Pipeline. If the incoming element has the provided key/value as check with
	 * .equals(), then let the element pass. If the key is id or label, then use respect id or label filtering.
	 *
	 * @param key
	 *            the property key to check
	 * @param compareToken
	 *            the comparison to use
	 * @param value
	 *            the object to filter on
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> has(String key, Tokens.T compareToken, Object value);

	/**
	 * Add an IdFilterPipe, LabelFilterPipe, or PropertyFilterPipe to the end of the Pipeline. If the incoming element has the provided key/value as check with
	 * .equals(), then let the element pass. If the key is id or label, then use respect id or label filtering.
	 *
	 * @param key
	 *            the property key to check
	 * @param predicate
	 *            the comparison to use
	 * @param value
	 *            the object to filter on
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> has(String key, Predicate predicate, Object value);

	/**
	 * If the incoming edge has the provided ferma_type property that is checked against the given class, then let the element pass.
	 * 
	 * @param clazz
	 *            the class that should be used for filtering
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> has(Class<?> clazz);

	/**
	 * Check if the element does not have a property with provided key.
	 *
	 * @param key
	 *            the property key to check
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> hasNot(String key);

	/**
	 * Add an IdFilterPipe, LabelFilterPipe, or PropertyFilterPipe to the end of the Pipeline. If the incoming element has the provided key/value as check with
	 * .equals(), then filter the element. If the key is id or label, then use respect id or label filtering.
	 *
	 * @param key
	 *            the property key to check
	 * @param value
	 *            the objects to filter on (in an OR manner)
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> hasNot(String key, Object value);

	/**
	 * Add an InVertexPipe to the end of the Pipeline. Emit the head vertex of the incoming edge.
	 *
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> inV();

	/**
	 * Add an OutVertexPipe to the end of the Pipeline. Emit the tail vertex of the incoming edge.
	 *
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> outV();

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a NoSuchElementException is thrown.
	 * 
	 * @param <T>
	 *            The type to frame the element as.
	 * @param kind
	 *            The type of frame for the element.
	 * @return the next emitted object
	 */
	<T> T next(Class<T> kind);

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a NoSuchElementException is thrown.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <T>
	 *            The type to frame the element as.
	 * @param kind
	 *            The type of frame for the element.
	 * @return the next emitted object
	 */
	<T> T nextExplicit(Class<T> kind);

	/**
	 * Return an iterator of framed elements.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <T>
	 *            The type to frame the element as.
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return An iterator of framed elements.
	 */
	<T> Iterable<T> frameExplicit(Class<T> kind);

	/**
	 * Return a list of all the objects in the pipeline.
	 * 
	 * @param <T>
	 *            The type to frame the element as.
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return a list of all the objects
	 */
	<T> List<? extends T> toList(Class<T> kind);

	/**
	 * Return a list of all the objects in the pipeline.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <T>
	 *            The type to frame the element as.
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return a list of all the objects
	 */
	<T> List<? extends T> toListExplicit(Class<T> kind);

	/**
	 * Add an LabelPipe to the end of the Pipeline. Emit the label of the incoming edge.
	 *
	 * @return the extended Pipeline
	 */
	Traversal<String, ?, ?, M> label();

	@Override
	EdgeTraversal<?, ?, M> filter(TraversalFunction<EdgeFrame, Boolean> filterFunction);

	/**
	 * Will emit the object only if it is in the provided array.
	 *
	 * @param edges
	 *            the edges to retain
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> retain(EdgeFrame... edges);

	/**
	 * Add an OrFilterPipe to the end the Pipeline. Will only emit the object if one or more of the provides pipes yields an object. The provided pipes are
	 * provided the object as their starts.
	 *
	 * @param pipes
	 *            the internal pipes of the OrFilterPipe
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> or(TraversalFunction<EdgeFrame, Traversal<?, ?, ?, ?>>... pipes);

	@Override
	EdgeTraversal<C, S, ? extends EdgeTraversal<C, S, M>> mark();

	/**
	 * Remove every element at the end of this Pipeline.
	 */
	void removeAll();

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

}
