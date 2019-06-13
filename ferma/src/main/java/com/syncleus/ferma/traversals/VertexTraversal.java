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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.VertexFrame;
import com.tinkerpop.blueprints.Predicate;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.Tokens;
import com.tinkerpop.pipes.transform.TransformPipe;

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
	 * If the incoming element has the provided key/value as check with .equals(), then let the element pass. If the key is id or label, then use respect id or
	 * label filtering.
	 *
	 * @param key
	 *            the property key to check
	 * @param predicate
	 *            the comparison to use
	 * @param value
	 *            the object to filter on
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> has(String key, Predicate predicate, Object value);

	/**
	 * If the incoming vertex has the provided ferma_type property that is checked against the given class, then let the element pass.
	 *
	 * @param clazz
	 *            the class to check against
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> has(Class<?> clazz);

	/**
	 * Check if the element does not have a property with provided key.
	 *
	 * @param key
	 *            the property key to check
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> hasNot(String key);

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
	 * If the incoming vertex has not the provided ferma_type property that is checked against the given class, then let it pass.
	 *
	 * @param clazz
	 *            the class to check against
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> hasNot(Class<?> clazz);

	/**
	 * If the incoming element has a value that is within the interval value range specified, then the element is allows to pass. If hte incoming element's
	 * value for the key is null, the element is filtered.
	 *
	 * @param <Z>
	 *            The type for the property values
	 * @param key
	 *            the property key to check
	 * @param startValue
	 *            the start of the interval (inclusive)
	 * @param endValue
	 *            the end of the interval (exclusive)
	 * @return the extended Pipeline
	 */
	<Z> VertexTraversal<?, ?, M> interval(String key, Comparable<Z> startValue, Comparable<Z> endValue);

	/**
	 * Emit the adjacent outgoing vertices of the incoming vertex.
	 *
	 * @param branchFactor
	 *            the number of max adjacent vertices for each incoming vertex
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> out(int branchFactor, String... labels);

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
	 * @param branchFactor
	 *            the number of max adjacent vertices for each incoming vertex
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> in(int branchFactor, String... labels);

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
	 * @param branchFactor
	 *            the number of max incident edges for each incoming vertex
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> outE(int branchFactor, String... labels);

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
	 * @param branchFactor
	 *            the number of max incident edges for each incoming vertex
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	EdgeTraversal<?, ?, M> inE(int branchFactor, String... labels);

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
	 * Get the next object emitted from the pipeline. If no such object exists a new vertex is created.
	 * 
	 * @return the next emitted object
	 */
	VertexFrame nextOrAdd();

	/**
	 * Get the next object emitted from the pipeline. If no such object exists a new vertex is created.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 * 
	 * @param <N>
	 *            The type used to frame the element
	 * @param initializer
	 *            the initializer for the frame which defines its type and may initialize properties
	 * @return the next emitted object
	 */
	<N> N nextOrAddExplicit(ClassInitializer<N> initializer);

	/**
	 * Get the next object emitted from the pipeline. If no such object exists a new vertex is created.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 * 
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The kind of frame.
	 * @return the next emitted object
	 */
	<N> N nextOrAddExplicit(Class<N> kind);

	/**
	 * Get the next object emitted from the pipeline. If no such object exists a new vertex is created.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param initializer
	 *            the initializer for the frame which defines its type and may initialize properties
	 * @return the next emitted object
	 */
	<N> N nextOrAdd(ClassInitializer<N> initializer);

	/**
	 * Get the next object emitted from the pipeline. If no such object exists a new vertex is created.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The kind of frame.
	 * @return the next emitted object
	 */
	<N> N nextOrAdd(Class<N> kind);

	/**
	 * Return the next X objects in the traversal as a list.
	 * 
	 * @param <N>
	 *            The type used to frame the element
	 * @param amount
	 *            the number of objects to return
	 * @param kind
	 *            the type of frame to for each element.
	 * @return a list of X objects (if X objects occur)
	 */
	<N> List<? extends N> next(int amount, Class<N> kind);

	/**
	 * Return the next X objects in the traversal as a list.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param amount
	 *            the number of objects to return
	 * @param kind
	 *            the type of frame to for each element.
	 * @return a list of X objects (if X objects occur)
	 */
	<N> List<? extends N> nextExplicit(int amount, Class<N> kind);

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
	<N> Iterable<? extends N> frameExplicit(Class<N> kind);

	/**
	 * Return a list of all the objects in the pipeline.
	 * 
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return a list of all the objects
	 * @deprecated Use Iterator and Traversal Result Instead
	 */
	@Deprecated
	<N> List<? extends N> toList(Class<N> kind);

	/**
	 * Return a list of all the objects in the pipeline.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return a list of all the objects
	 * @deprecated Use Iterator and Traversal Result Instead
	 */
	@Deprecated
	<N> List<? extends N> toListExplicit(Class<N> kind);

	/**
	 * Emit the incoming vertex, but have other vertex provide an outgoing edge to incoming vertex.
	 *
	 * @param label
	 *            the edge label
	 * @param namedStep
	 *            the step name that has the other vertex to link to
	 * @return the extended Pipeline
	 */
	VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkOut(String label, String namedStep);

	/**
	 * Emit the incoming vertex, but have other vertex provide an incoming edge to incoming vertex.
	 *
	 * @param label
	 *            the edge label
	 * @param namedStep
	 *            the step name that has the other vertex to link to
	 * @return the extended Pipeline
	 */
	VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkIn(String label, String namedStep);

	/**
	 * Emit the incoming vertex, but have other vertex provide an incoming and outgoing edge to incoming vertex.
	 *
	 * @param label
	 *            the edge label
	 * @param namedStep
	 *            the step name that has the other vertex to link to
	 * @return the extended Pipeline
	 */
	VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkBoth(String label, String namedStep);

	/**
	 * Emit the incoming vertex, but have other vertex provide an outgoing edge to incoming vertex.
	 *
	 * @param label
	 *            the edge label
	 * @param other
	 *            the other vertex
	 * @return the extended Pipeline
	 */
	VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkOut(String label, Vertex other);

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
	VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkIn(String label, Vertex other);

	/**
	 * Emit the incoming vertex, but have other vertex provide an incoming and outgoing edge to incoming vertex.
	 *
	 * @param label
	 *            the edge label
	 * @param other
	 *            the other vertex
	 * @return the extended Pipeline
	 */
	VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkBoth(String label, Vertex other);

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

	/**
	 * Emit the incoming vertex, but have other vertex provide an incoming and outgoing edge to incoming vertex.
	 *
	 * @param label
	 *            the edge label
	 * @param other
	 *            the other vertex
	 * @return the extended Pipeline
	 */
	VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkBoth(String label, VertexFrame other);

	@Override
	VertexTraversal<?, ?, M> dedup(TraversalFunction<VertexFrame, ?> dedupFunction);

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
	VertexTraversal<?, ?, M> identity();

	@Override
	VertexTraversal<?, ?, M> order();

	@Override
	VertexTraversal<?, ?, M> order(Comparator<? super VertexFrame> compareFunction);

	@Override
	VertexTraversal<?, ?, M> order(TransformPipe.Order order);

	@Override
	VertexTraversal<?, ?, M> order(Tokens.T order);

	/**
	 * Fill the provided collection with the objects in the pipeline.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param collection
	 *            the collection to fill
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return the collection filled
	 */
	<N> Collection<? extends N> fill(Collection<? super N> collection, Class<N> kind);

	/**
	 * Fill the provided collection with the objects in the pipeline.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param collection
	 *            the collection to fill
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return the collection filled
	 */
	<N> Collection<? extends N> fillExplicit(Collection<? super N> collection, Class<N> kind);

	/**
	 * If the internal pipes all yield objects, then the object is not filtered. The provided pipes are provided the object as their starts.
	 *
	 * @param traversals
	 *            the internal pipes of the AndFilterPipe
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> and(TraversalFunction<VertexFrame, Traversal<?, ?, ?, ?>>... traversals);

	/**
	 * Will only emit the object if one or more of the provides pipes yields an object. The provided pipes are provided the object as their starts.
	 *
	 * @param traversals
	 *            the internal pipes of the OrFilterPipe
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> or(TraversalFunction<VertexFrame, Traversal<?, ?, ?, ?>>... traversals);

	@Override
	VertexTraversal<C, S, ? extends VertexTraversal<C, S, M>> mark();

	/**
	 * Remove every element at the end of this Pipeline.
	 */
	void removeAll();

	/**
	 * The incoming objects are copied to the provided pipes. This "split-pipe" is used in conjunction with some type of "merge-pipe."
	 *
	 * @param <N>
	 *            The type of the objects through the traversal.
	 * @param traversals
	 *            the internal pipes of the CopySplitPipe
	 * @return the extended Pipeline
	 */
	<N> SplitTraversal<? extends Traversal<N, ?, ?, M>> copySplit(TraversalFunction<VertexFrame, ? extends Traversal<N, ?, ?, ?>>... traversals);

	/**
	 * The pipeline loops over the supplied traversal.
	 *
	 * @param traversal
	 *            the traversal to look over.
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> loop(TraversalFunction<VertexFrame, ? extends VertexTraversal<?, ?, ?>> traversal);

	/**
	 * The pipeline loops over the supplied traversal up to a maximum depth.
	 *
	 * @param traversal
	 *            the traversal to look over.
	 * @param depth
	 *            The maximum depth to loop to
	 * @return the extended Pipeline
	 */
	VertexTraversal<?, ?, M> loop(TraversalFunction<VertexFrame, ? extends VertexTraversal<?, ?, ?>> traversal, int depth);

}
