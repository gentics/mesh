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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.syncleus.ferma.EdgeFrame;
import com.tinkerpop.blueprints.Predicate;
import com.tinkerpop.gremlin.Tokens;
import com.tinkerpop.pipes.transform.TransformPipe;
import com.tinkerpop.pipes.util.structures.Pair;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.pipes.util.structures.Tree;

/**
 * Edge specific traversal.
 *
 * @param <C> The cap of the current pipe.
 * @param <S> The SideEffect of the current pipe.
 * @param <M> The current mark'ed type for the current pipe.
 */
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
     * Add an IdFilterPipe, LabelFilterPipe, or PropertyFilterPipe to the end of
     * the Pipeline. If the incoming element has the provided key/value as check
     * with .equals(), then let the element pass. If the key is id or label,
     * then use respect id or label filtering.
     *
     * @param key
     *            the property key to check
     * @param value
     *            the object to filter on (in an OR manner)
     * @return the extended Pipeline
     */
    EdgeTraversal<?, ?, M> has(String key, Object value);

    /**
     * Add an IdFilterPipe, LabelFilterPipe, or PropertyFilterPipe to the end of
     * the Pipeline. If the incoming element has the provided key/value as check
     * with .equals(), then let the element pass. If the key is id or label,
     * then use respect id or label filtering.
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
     * Add an IdFilterPipe, LabelFilterPipe, or PropertyFilterPipe to the end of
     * the Pipeline. If the incoming element has the provided key/value as check
     * with .equals(), then let the element pass. If the key is id or label,
     * then use respect id or label filtering.
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
     * If the incoming edge has the provided ferma_type property that is
     * checked against the given class, then let the element pass.
     * 
     * @param clazz
     *             the class that should be used for filtering
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
     * Add an IdFilterPipe, LabelFilterPipe, or PropertyFilterPipe to the end of
     * the Pipeline. If the incoming element has the provided key/value as check
     * with .equals(), then filter the element. If the key is id or label, then
     * use respect id or label filtering.
     *
     * @param key
     *            the property key to check
     * @param value
     *            the objects to filter on (in an OR manner)
     * @return the extended Pipeline
     */
    EdgeTraversal<?, ?, M> hasNot(String key, Object value);

    /**
     * If the incoming edge has not the provided ferma_type property that is
     * checked against the given class, then let the edge pass.
     *
     * @param clazz
     *             the class to check against
     * @return the extended Pipeline
     */
    EdgeTraversal<?, ?, M> hasNot(Class<?> clazz);

    /**
     * Add an IntervalFilterPipe to the end of the Pipeline. If the incoming
     * element has a value that is within the interval value range specified,
     * then the element is allows to pass. If the incoming element's value for
     * the key is null, the element is filtered.
     *
     * @param <Z> The type for the property value.
     * @param key
     *            the property key to check
     * @param startValue
     *            the start of the interval (inclusive)
     * @param endValue
     *            the end of the interval (exclusive)
     * @return the extended Pipeline
     */
    <Z> EdgeTraversal<?, ?, M> interval(String key, Comparable<Z> startValue, Comparable<Z> endValue);

    /**
     * Add an InVertexPipe to the end of the Pipeline. Emit the head vertex of
     * the incoming edge.
     *
     * @return the extended Pipeline
     */
    VertexTraversal<?, ?, M> inV();

    /**
     * Add an OutVertexPipe to the end of the Pipeline. Emit the tail vertex of
     * the incoming edge.
     *
     * @return the extended Pipeline
     */
    VertexTraversal<?, ?, M> outV();

    /**
     * Emit both the tail and head vertices of the incoming edge.
     *
     * @return the extended Pipeline
     */
    VertexTraversal<?, ?, M> bothV();

    /**
     * Get the next object emitted from the pipeline. If no such object exists,
     * then a NoSuchElementException is thrown.
     * 
     * @param <T> The type to frame the element as.
     * @param kind
     *            The type of frame for the element.
     * @return the next emitted object
     */
    <T> T next(Class<T> kind);

    /**
     * Get the next object emitted from the pipeline. If no such object exists,
     * then a NoSuchElementException is thrown.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <T> The type to frame the element as.
     * @param kind
     *            The type of frame for the element.
     * @return the next emitted object
     */
    <T> T nextExplicit(Class<T> kind);

    /**
     * Return the next X objects in the traversal as a list.
     * 
     * @param <T> The type to frame the element as.
     * @param amount
     *            the number of objects to return
     * @param kind
     *            the type of frame to for each element.
     * @return a list of X objects (if X objects occur)
     */
    <T> List<? extends T> next(int amount, Class<T> kind);

    /**
     * Return the next X objects in the traversal as a list.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <T> The type to frame the element as.
     * @param amount
     *            the number of objects to return
     * @param kind
     *            the type of frame to for each element.
     * @return a list of X objects (if X objects occur)
     */
    <T> List<? extends T> nextExplicit(int amount, Class<T> kind);

    /**
     * Return an iterator of framed elements.
     * 
     * @param <T> The type to frame the element as.
     * @param kind
     *            The kind of framed elements to return.
     * @return An iterator of framed elements.
     */
    <T> Iterable<T> frame(Class<T> kind);

    /**
     * Return an iterator of framed elements.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <T> The type to frame the element as.
     * @param kind
     *            The kind of framed elements to return.
     * @return An iterator of framed elements.
     */
    <T> Iterable<T> frameExplicit(Class<T> kind);

    /**
     * Return a list of all the objects in the pipeline.
     * 
     * @param <T> The type to frame the element as.
     * @param kind
     *            The kind of framed elements to return.
     * @return a list of all the objects
     */
    <T> List<? extends T> toList(Class<T> kind);

    /**
     * Return a set of all the objects in the pipeline.
     *
     * @param <T> The type to frame the element as.
     * @param kind
     *            The kind of framed elements to return.
     * @return a set of all the objects
     */
    <T> Set<? extends T> toSet(Class<T> kind);

    /**
     * Return a set of all the objects in the pipeline.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <T> The type to frame the element as.
     * @param kind
     *            The kind of framed elements to return.
     * @return a set of all the objects
     */
    <T> Set<? extends T> toSetExplicit(Class<T> kind);

    /**
     * Return a list of all the objects in the pipeline.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <T> The type to frame the element as.
     * @param kind
     *            The kind of framed elements to return.
     * @return a list of all the objects
     */
    <T> List<? extends T> toListExplicit(Class<T> kind);

    /**
     * Add an LabelPipe to the end of the Pipeline. Emit the label of the
     * incoming edge.
     *
     * @return the extended Pipeline
     */
    Traversal<String, ?, ?, M> label();

    @Override
    EdgeTraversal<?, ?, M> dedup();

    @Override
    EdgeTraversal<?, ?, M> dedup(TraversalFunction<EdgeFrame, ?> dedupFunction);

    @Override
    EdgeTraversal<?, ?, M> except(Iterable<?> collection);

    /**
     * Add an ExceptFilterPipe to the end of the Pipeline. Will only emit the
     * object if it is not in the provided array.
     *
     * @param edges
     *            the edges to reject from the stream
     * @return the extended Pipeline
     */
    EdgeTraversal<?, ?, M> except(EdgeFrame... edges);

    @Override
    EdgeTraversal<?, ?, M> except(String... namedSteps);

    @Override
    EdgeTraversal<?, ?, M> filter(TraversalFunction<EdgeFrame, Boolean> filterFunction);

    @Override
    EdgeTraversal<?, ?, M> random(double bias);

    @Override
    EdgeTraversal<?, ?, M> range(int low, int high);

    @Override
    EdgeTraversal<?, ?, M> limit(int limit);

    @Override
    EdgeTraversal<?, ?, M> retain(Iterable<?> collection);

    /**
     * Will emit the object only if it is in the provided array.
     *
     * @param edges
     *            the edges to retain
     * @return the extended Pipeline
     */
    EdgeTraversal<?, ?, M> retain(EdgeFrame... edges);

    @Override
    EdgeTraversal<?, ?, M> retain(String... namedSteps);

    @Override
    EdgeTraversal<Collection<? extends EdgeFrame>, Collection<? extends EdgeFrame>, M> aggregate();

    @Override
    EdgeTraversal<Collection<? extends EdgeFrame>, Collection<? extends EdgeFrame>, M> aggregate(Collection<? super EdgeFrame> aggregate);

    @Override
    <N> EdgeTraversal<Collection<? extends N>, Collection<? extends N>, M> aggregate(Collection<? super N> aggregate, TraversalFunction<EdgeFrame, ? extends N> aggregateFunction);

    @Override
    <N> EdgeTraversal<Collection<? extends N>, Collection<? extends N>, M> aggregate(TraversalFunction<EdgeFrame, ? extends N> aggregateFunction);

    @Override
    <K, V> EdgeTraversal<Map<K, List<V>>, Map<K, List<V>>, M> groupBy(Map<K, List<V>> map, TraversalFunction<EdgeFrame, K> keyFunction, TraversalFunction<EdgeFrame, Iterator<V>> valueFunction);

    @Override
    <K, V> EdgeTraversal<Map<K, List<V>>, Map<K, List<V>>, M> groupBy(TraversalFunction<EdgeFrame, K> keyFunction, TraversalFunction<EdgeFrame, Iterator<V>> valueFunction);

    @Override
    <K, V, V2> EdgeTraversal<Map<K, V2>, Map<K, V2>, M> groupBy(Map<K, V2> reduceMap, TraversalFunction<EdgeFrame, K> keyFunction, TraversalFunction<EdgeFrame, Iterator<V>> valueFunction,
                                                                TraversalFunction<List<V>, V2> reduceFunction);

    @Override
    <K, V, V2> EdgeTraversal<Map<K, V2>, Map<K, V2>, M> groupBy(TraversalFunction<EdgeFrame, K> keyFunction, TraversalFunction<EdgeFrame, Iterator<V>> valueFunction,
                                                                TraversalFunction<List<V>, V2> reduceFunction);

    @Override
    <K> EdgeTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(Map<K, Long> map,
                                                                TraversalFunction<EdgeFrame, K> keyFunction, TraversalFunction<Pair<EdgeFrame, Long>, Long> valueFunction);

    @Override
    <K> EdgeTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(TraversalFunction<EdgeFrame, K> keyFunction, TraversalFunction<Pair<EdgeFrame, Long>, Long> valueFunction);

    @Override
    <K> EdgeTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(Map<K, Long> map,
                                                                TraversalFunction<EdgeFrame, K> keyFunction);

    @Override
    <K> EdgeTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(TraversalFunction<EdgeFrame, K> keyFunction);

    @Override
    EdgeTraversal<Map<EdgeFrame, Long>, Map<EdgeFrame, Long>, M> groupCount(Map<EdgeFrame, Long> map);

    @Override
    EdgeTraversal<Map<EdgeFrame, Long>, Map<EdgeFrame, Long>, M> groupCount();

    @Override
    EdgeTraversal<?, ?, M> sideEffect(SideEffectFunction<EdgeFrame> sideEffectFunction);

    @Override
    <N> Traversal<EdgeFrame, Collection<? extends N>, N, M> store(Collection<? super N> storage);

    @Override
    <N> Traversal<EdgeFrame, Collection<? extends N>, N, M> store(Collection<? super N> storage,
                                                                  TraversalFunction<EdgeFrame, N> storageFunction);

    @Override
    Traversal<EdgeFrame, Collection<? extends EdgeFrame>, EdgeFrame, M> store();

    @Override
    <N> Traversal<EdgeFrame, Collection<? extends N>, N, M> store(TraversalFunction<EdgeFrame, N> storageFunction);

    @Override
    EdgeTraversal<Table, Table, M> table(Table table, Collection<String> stepNames,
                                         TraversalFunction<?, ?>... columnFunctions);

    @Override
    EdgeTraversal<Table, Table, M> table(Table table, TraversalFunction<?, ?>... columnFunctions);

    @Override
    EdgeTraversal<Table, Table, M> table(TraversalFunction<?, ?>... columnFunctions);

    @Override
    EdgeTraversal<Table, Table, M> table(Table table);

    @Override
    EdgeTraversal<Table, Table, M> table();

    @Override
    <N> EdgeTraversal<Tree<N>, Tree<N>, M> tree(Tree<N> tree);

    @Override
    EdgeTraversal<?, ?, M> identity();

    @Override
    EdgeTraversal<?, ?, M> memoize(String namedStep);

    @Override
    EdgeTraversal<?, ?, M> memoize(String namedStep, Map<?, ?> map);

    @Override
    EdgeTraversal<?, ?, M> order();

    @Override
    EdgeTraversal<?, ?, M> order(TransformPipe.Order order);

    @Override
    EdgeTraversal<?, ?, M> order(Comparator<? super EdgeFrame> compareFunction);

    @Override
    EdgeTraversal<?, ?, M> as(String name);

    @Override
    EdgeTraversal<?, ?, M> simplePath();

    /**
     * Fill the provided collection with the objects in the pipeline.
     *
     * @param <N> the type used to frame the elements in the pipeline.
     * @param collection
     *            the collection to fill
     * @param kind
     *            The kind of framed elements to return.
     * @return the collection filled
     */
    <N> Collection<N> fill(Collection<? super N> collection, Class<N> kind);

    /**
     * Fill the provided collection with the objects in the pipeline.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <N> the type used to frame the elements in the pipeline.
     * @param collection
     *            the collection to fill
     * @param kind
     *            The kind of framed elements to return.
     * @return the collection filled
     */
    <N> Collection<N> fillExplicit(Collection<? super N> collection, Class<N> kind);

    @Override
    EdgeTraversal<?, ?, M> gatherScatter();

    /**
     * Add an AndFilterPipe to the end the Pipeline. If the internal pipes all
     * yield objects, then the object is not filtered. The provided pipes are
     * provided the object as their starts.
     *
     * @param pipes
     *            the internal pipes of the AndFilterPipe
     * @return the extended Pipeline
     */
    EdgeTraversal<?, ?, M> and(TraversalFunction<EdgeFrame, Traversal<?, ?, ?, ?>>... pipes);

    /**
     * Add an OrFilterPipe to the end the Pipeline. Will only emit the object if
     * one or more of the provides pipes yields an object. The provided pipes
     * are provided the object as their starts.
     *
     * @param pipes
     *            the internal pipes of the OrFilterPipe
     * @return the extended Pipeline
     */
    EdgeTraversal<?, ?, M> or(TraversalFunction<EdgeFrame, Traversal<?, ?, ?, ?>>... pipes);

    @Override
    EdgeTraversal<?, ?, M> divert(SideEffectFunction<S> sideEffectFunction);

    @Override
    EdgeTraversal<?, ?, M> shuffle();

    @Override
    EdgeTraversal<C, S, ? extends EdgeTraversal<C, S, M>> mark();

    /**
     * Remove every element at the end of this Pipeline.
     */
    void removeAll();

    /**
     * The incoming objects are copied to the provided pipes. This "split-pipe"
     * is used in conjunction with some type of "merge-pipe."
     *
     * @param <N> The type of the element being traversed.
     * @param traversals
     *            the internal pipes of the CopySplitPipe
     * @return the extended Pipeline
     */
    <N> SplitTraversal<? extends Traversal<N, ?, ?, M>> copySplit(TraversalFunction<EdgeFrame, ? extends Traversal<N, ?, ?, ?>>... traversals);

    /**
     * The pipeline loops over the supplied traversal.
     *
     * @param traversal the traversal to loop over.
     * @return the extended Pipeline
     */
    EdgeTraversal<?, ?, M> loop(TraversalFunction<EdgeFrame, ? extends EdgeTraversal<?, ?, ?>> traversal);

    /**
     * The pipeline loops over the supplied traversal up to a maximum depth.
     *
     * @param traversal the traversal to loop over.
     * @param depth The maximum depth to loop to
     * @return the extended Pipeline
     */
    EdgeTraversal<?, ?, M> loop(TraversalFunction<EdgeFrame, ? extends EdgeTraversal<?, ?, ?>> traversal, int depth);

    /**
     * Get the next object emitted from the pipeline. If no such object exists,
     * then a the default value is returned.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <N> The type used to frame the element
     * @param kind
     *            The type of frame for the element.
     * @param defaultValue
     *            The object to return if no next object exists.
     * @return the next emitted object
     */
    <N> N nextOrDefaultExplicit(Class<N> kind, N defaultValue);

}
