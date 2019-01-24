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

import java.util.*;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.pipes.FermaGremlinPipeline;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Predicate;
import com.tinkerpop.gremlin.Tokens;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.branch.LoopPipe;
import com.tinkerpop.pipes.transform.TransformPipe.Order;
import com.tinkerpop.pipes.util.structures.Pair;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.pipes.util.structures.Tree;

/**
 * Edge specific traversal.
 * 
 * @param <C> The cap of the current pipe.
 * @param <S> The SideEffect of the current pipe.
 * @param <M> The current marked type for the current pipe.
 */
abstract class AbstractEdgeTraversal<C, S, M> extends AbstractTraversal<EdgeFrame, C, S, M> implements EdgeTraversal<C, S, M> {
    protected AbstractEdgeTraversal(FramedGraph graph, FermaGremlinPipeline pipeline) {
        super(graph, pipeline);
    }

    @Override
    public EdgeFrame next() {

        return graph().frameElement((Edge) getPipeline().next(), EdgeFrame.class);
    }

    @Override
    public List<? extends EdgeFrame> toList() {
        return toListExplicit(EdgeFrame.class);
    }

    @Override
    public Set<? extends EdgeFrame> toSet() {
        return toSetExplicit(EdgeFrame.class);
    }

    @Override
    public EdgeTraversal<Table, Table, M> table() {
        return (EdgeTraversal) super.table();
    }

    @Override
    public EdgeTraversal<Table, Table, M> table(final TraversalFunction... columnFunctions) {

        return (EdgeTraversal) super.table(columnFunctions);
    }

    @Override
    public EdgeTraversal<Table, Table, M> table(final Table table) {

        return (EdgeTraversal) super.table(table);
    }

    @Override
    public EdgeTraversal<Table, Table, M> table(final Table table, final Collection stepNames, final TraversalFunction... columnFunctions) {

        return (EdgeTraversal) super.table(table, stepNames, columnFunctions);
    }

    @Override
    public EdgeTraversal<Table, Table, M> table(final Table table, final TraversalFunction... columnFunctions) {
        return (EdgeTraversal) super.table(table, columnFunctions);
    }

    @Override
    public <N> EdgeTraversal<Tree<N>, Tree<N>, M> tree(final Tree<N> tree) {
        return (EdgeTraversal) super.tree(tree);
    }

    @Override
    public Traversal<EdgeFrame, Collection<? extends EdgeFrame>, EdgeFrame, M> store() {
        return (EdgeTraversal) super.store();
    }

    @Override
    public <N> Traversal<EdgeFrame, Collection<? extends N>, N, M> store(final Collection<? super N> storage) {
        return (EdgeTraversal) super.store(storage);
    }

    @Override
    public <N> Traversal<EdgeFrame, Collection<? extends N>, N, M> store(final Collection<? super N> storage, final TraversalFunction<EdgeFrame, N> storageFunction) {
        return (EdgeTraversal) super.store(storage, storageFunction);
    }

    @Override
    public <N> Traversal<EdgeFrame, Collection<? extends N>, N, M> store(final TraversalFunction<EdgeFrame, N> storageFunction) {
        return (EdgeTraversal) super.store(storageFunction);
    }

    @Override
    public EdgeTraversal<Map<EdgeFrame, Long>, Map<EdgeFrame, Long>, M> groupCount() {
        return (EdgeTraversal) super.groupCount();
    }

    @Override
    public EdgeTraversal<Map<EdgeFrame, Long>, Map<EdgeFrame, Long>, M> groupCount(final Map<EdgeFrame, Long> map) {
        return (EdgeTraversal) super.groupCount(map);
    }

    @Override
    public <K> EdgeTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(final Map<K, Long> map, final TraversalFunction<EdgeFrame, K> keyFunction) {
        return (EdgeTraversal) super.groupCount(map, keyFunction);
    }

    @Override
    public <K> EdgeTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(final Map<K, Long> map, final TraversalFunction<EdgeFrame, K> keyFunction,
                                                                       final TraversalFunction<Pair<EdgeFrame, Long>, Long> valueFunction) {
        return (EdgeTraversal) super.groupCount(map, keyFunction, valueFunction);
    }

    @Override
    public <K> EdgeTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(final TraversalFunction<EdgeFrame, K> keyFunction) {
        return (EdgeTraversal) super.groupCount(keyFunction);
    }

    @Override
    public <K> EdgeTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(final TraversalFunction<EdgeFrame, K> keyFunction, final TraversalFunction<Pair<EdgeFrame, Long>, Long> valueFunction) {
        return (EdgeTraversal) super.groupCount(keyFunction, valueFunction);
    }

    @Override
    public <K, V, V2> EdgeTraversal<Map<K, V2>, Map<K, V2>, M> groupBy(final TraversalFunction<EdgeFrame, K> keyFunction, final TraversalFunction<EdgeFrame, Iterator<V>> valueFunction,
                                                                       final TraversalFunction<List<V>, V2> reduceFunction) {
        return (EdgeTraversal) super.groupBy(keyFunction, valueFunction, reduceFunction);
    }

    @Override
    public <K, V> EdgeTraversal<Map<K, List<V>>, Map<K, List<V>>, M> groupBy(final TraversalFunction<EdgeFrame, K> keyFunction, final TraversalFunction<EdgeFrame, Iterator<V>> valueFunction) {

        return (EdgeTraversal) super.groupBy(keyFunction, valueFunction);
    }

    @Override
    public <K, V, V2> EdgeTraversal<Map<K, V2>, Map<K, V2>, M> groupBy(final Map<K, V2> reduceMap, final TraversalFunction<EdgeFrame, K> keyFunction,
                                                                       final TraversalFunction<EdgeFrame, Iterator<V>> valueFunction, final TraversalFunction<List<V>, V2> reduceFunction) {
        return (EdgeTraversal) super.groupBy(reduceMap, keyFunction, valueFunction, reduceFunction);
    }

    @Override
    public <K, V> EdgeTraversal<Map<K, List<V>>, Map<K, List<V>>, M> groupBy(final Map<K, List<V>> map, final TraversalFunction<EdgeFrame, K> keyFunction,
                                                                             final TraversalFunction<EdgeFrame, Iterator<V>> valueFunction) {
        return (EdgeTraversal) super.groupBy(map, keyFunction, valueFunction);
    }

    @Override
    public EdgeTraversal<?, ?, M> has(final String key) {
        return (EdgeTraversal) super.has(key);
    }

    @Override
    public EdgeTraversal<?, ?, M> has(final String key, final Object value) {
        return (EdgeTraversal) super.has(key, value);
    }

    @Override
    public EdgeTraversal<?, ?, M> has(final String key, final Predicate predicate, final Object value) {
        return (EdgeTraversal) super.has(key, predicate, value);
    }

    @Override
    public EdgeTraversal<?, ?, M> has(final String key, final com.tinkerpop.gremlin.Tokens.T compareToken, final Object value) {
        return (EdgeTraversal) super.has(key, compareToken, value);
    }

    @Override
    public EdgeTraversal<?, ?, M> has(Class<?> clazz) {
        graph().getTypeResolver().hasType(this, clazz);
        return this;
    }

    @Override
    public EdgeTraversal<?, ?, M> hasNot(final String key) {
        return (EdgeTraversal) super.hasNot(key);
    }

    @Override
    public EdgeTraversal<?, ?, M> hasNot(final String key, final Object value) {
        return (EdgeTraversal) super.hasNot(key, value);
    }

    @Override
    public EdgeTraversal<?, ?, M> hasNot(Class<?> clazz) {
        graph().getTypeResolver().hasNotType(this, clazz);
        return this;
    }

    @Override
    public EdgeTraversal<?, ?, M> as(final String name) {
        return (EdgeTraversal) super.as(name);
    }

    @Override
    public EdgeTraversal<?, ?, M> identity() {

        return (EdgeTraversal) super.identity();
    }

    @Override
    public EdgeTraversal<?, ?, M> interval(final String key, final Comparable startValue, final Comparable endValue) {
        return (EdgeTraversal) super.interval(key, startValue, endValue);
    }

    @Override
    public VertexTraversal<?, ?, M> inV() {
        getPipeline().inV();
        return castToVertices();
    }

    @Override
    public VertexTraversal<?, ?, M> outV() {
        getPipeline().outV();
        return castToVertices();
    }

    @Override
    public VertexTraversal<?, ?, M> bothV() {
        getPipeline().bothV();
        return castToVertices();
    }

    @Override
    public <T> T next(final Class<T> kind) {
        return graph().frameElement((Element) getPipeline().next(), kind);
    }

    @Override
    public <T> T nextExplicit(final Class<T> kind) {
        return graph().frameElementExplicit((Element) getPipeline().next(), kind);
    }

    @Override
    public <T> List<? extends T> next(final int amount, final Class<T> kind) {
        return Lists.transform(getPipeline().next(amount), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElement((Element) input, kind);
            }
        });
    }

    @Override
    public <T> List<? extends T> nextExplicit(final int amount, final Class<T> kind) {
        return Lists.transform(getPipeline().next(amount), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElementExplicit((Element) input, kind);
            }
        });
    }

    @Override
    public <N> N nextOrDefaultExplicit(final Class<N> kind, final N defaultValue) {
        if (getPipeline().hasNext()) {
            return nextExplicit(kind);
        } else {
            return defaultValue;
        }
    }

    @Override
    public <T> Iterable<T> frame(final Class<T> kind) {
        return Iterables.transform(getPipeline(), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElement((Element) input, kind);
            }
        });
    }

    @Override
    public <T> Iterable<T> frameExplicit(final Class<T> kind) {
        return Iterables.transform(getPipeline(), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElementExplicit((Element) input, kind);
            }
        });
    }

    @Override
    public <T> List<? extends T> toList(final Class<T> kind) {
        return Lists.transform(getPipeline().toList(), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElement((Element) input, kind);
            }
        });
    }

    @Override
    public <T> List<? extends T> toListExplicit(final Class<T> kind) {
        return Lists.transform(getPipeline().toList(), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElementExplicit((Element) input, kind);
            }
        });
    }

    @Override
    public <T> Set<? extends T> toSet(final Class<T> kind) {
        return Sets.newHashSet(toList(kind));
    }

    @Override
    public <T> Set<? extends T> toSetExplicit(final Class<T> kind) {
        return Sets.newHashSet(toListExplicit(kind));
    }

    @Override
    public Traversal<String, ?, ?, M> label() {
        getPipeline().label();
        return castToTraversal();
    }

    @Override
    public EdgeTraversal<?, ?, M> filter(final TraversalFunction<EdgeFrame, Boolean> filterFunction) {
        return (EdgeTraversal<?, ?, M>) super.filter(filterFunction);

    }

    @Override
    public EdgeTraversal<Collection<? extends EdgeFrame>, Collection<? extends EdgeFrame>, M> aggregate() {
        return (EdgeTraversal<Collection<? extends EdgeFrame>, Collection<? extends EdgeFrame>, M>) super.aggregate();

    }

    @Override
    public EdgeTraversal<Collection<? extends EdgeFrame>, Collection<? extends EdgeFrame>, M> aggregate(final Collection<? super EdgeFrame> aggregate) {
        return (EdgeTraversal<Collection<? extends EdgeFrame>, Collection<? extends EdgeFrame>, M>) super.aggregate(aggregate);

    }

    @Override
    public <N> EdgeTraversal<Collection<? extends N>, Collection<? extends N>, M> aggregate(final Collection<? super N> aggregate, final TraversalFunction<EdgeFrame, ? extends N> aggregateFunction) {
        Traversal<EdgeFrame, ?, ?, M> traversal = super.aggregate(aggregate, aggregateFunction);
        return (EdgeTraversal<Collection<? extends N>, Collection<? extends N>, M>)traversal;

    }

    @Override
    public <N> EdgeTraversal<Collection<? extends N>, Collection<? extends N>, M> aggregate(final TraversalFunction<EdgeFrame, ? extends N> aggregateFunction) {
        Traversal<EdgeFrame, ?, ?, M> traversal = super.aggregate(aggregateFunction);
        return (EdgeTraversal<Collection<? extends N>, Collection<? extends N>, M>)traversal;

    }

    @Override
    public EdgeTraversal<?, ?, M> and(final TraversalFunction<EdgeFrame, Traversal<?, ?, ?, ?>>... pipes) {
        final Collection<Pipe> extractedPipes = Collections2.transform(Arrays.asList(pipes), new Function<TraversalFunction, Pipe>() {

            @Override
            public Pipe apply(final TraversalFunction input) {
                return ((AbstractTraversal) input.compute(new TEdge())).getPipeline();
            }
        });
        getPipeline().and(extractedPipes.toArray(new Pipe[extractedPipes.size()]));
        return this;
    }

    @Override
    public EdgeTraversal<?, ?, M> or(final TraversalFunction<EdgeFrame, Traversal<?, ?, ?, ?>>... pipes) {
        final Collection<Pipe> extractedPipes = Collections2.transform(Arrays.asList(pipes), new Function<TraversalFunction, Pipe>() {

            @Override
            public Pipe apply(final TraversalFunction input) {
                return ((AbstractTraversal) input.compute(new TEdge())).getPipeline();
            }
        });
        getPipeline().or(extractedPipes.toArray(new Pipe[extractedPipes.size()]));
        return this;
    }

    @Override
    public EdgeTraversal<?, ?, M> sideEffect(final SideEffectFunction<EdgeFrame> sideEffectFunction) {
        return (EdgeTraversal<?, ?, M>) super.sideEffect(sideEffectFunction);
    }

    @Override
    public EdgeTraversal<?, ?, M> random(final double bias) {

        return (EdgeTraversal<?, ?, M>) super.random(bias);
    }

    @Override
    public EdgeTraversal<?, ?, M> dedup(final TraversalFunction<EdgeFrame, ?> dedupFunction) {
        return (EdgeTraversal<?, ?, M>) super.dedup(dedupFunction);
    }

    @Override
    public EdgeTraversal<?, ?, M> except(final String... namedSteps) {

        return (EdgeTraversal<?, ?, M>) super.except(namedSteps);
    }

    @Override
    public EdgeTraversal<?, ?, M> except(final Iterable<?> collection) {
        return (EdgeTraversal) super.except(Lists.newArrayList(collection));
    }

    @Override
    public EdgeTraversal<?, ?, M> range(final int low, final int high) {
        return (EdgeTraversal<?, ?, M>) super.range(low, high);
    }

    @Override
    public EdgeTraversal<?, ?, M> order() {
        return (EdgeTraversal<?, ?, M>) super.order();
    }

    @Override
    public EdgeTraversal<?, ?, M> order(final Order order) {
        return (EdgeTraversal<?, ?, M>) super.order(order);
    }

    @Override
    public EdgeTraversal<?, ?, M> order(final Comparator<? super EdgeFrame> compareFunction) {
        return (EdgeTraversal<?, ?, M>) super.order(compareFunction);
    }

    @Override
    public EdgeTraversal<?, ?, M> order(final Tokens.T order) {
        return (EdgeTraversal<?, ?, M>) super.order(order);
    }

    @Override
    public EdgeTraversal<?, ?, M> dedup() {

        return (EdgeTraversal<?, ?, M>) super.dedup();
    }

    @Override
    public EdgeTraversal<?, ?, M> retain(final String... namedSteps) {
        return (EdgeTraversal<?, ?, M>) super.retain(namedSteps);
    }

    @Override
    public EdgeTraversal<?, ?, M> simplePath() {
        return (EdgeTraversal<?, ?, M>) super.simplePath();
    }

    @Override
    public EdgeTraversal<?, ?, M> memoize(final String namedStep) {
        return (EdgeTraversal<?, ?, M>) super.memoize(namedStep);

    }

    @Override
    public EdgeTraversal<?, ?, M> memoize(final String namedStep, final Map<?, ?> map) {
        return (EdgeTraversal<?, ?, M>) super.memoize(namedStep, map);

    }

    @Override
    public Collection<? extends EdgeFrame> fill(final Collection<? super EdgeFrame> collection) {
        return super.fill(collection);
    }

    @Override
    public <N> Collection<N> fill(final Collection<? super N> collection, final Class<N> kind) {

        return getPipeline().fill(new FramingCollection(collection, graph(), kind));
    }

    @Override
    public <N> Collection<N> fillExplicit(final Collection<? super N> collection, final Class<N> kind) {

        return getPipeline().fill(new FramingCollection(collection, graph(), kind, true));
    }

    @Override
    public java.util.Iterator<EdgeFrame> iterator() {
        return Iterators.transform(getPipeline(), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElement((Element) input, EdgeFrame.class);
            }
        });
    }

    @Override
    public EdgeTraversal<?, ?, M> gatherScatter() {

        return (EdgeTraversal<?, ?, M>) super.gatherScatter();
    }

    @Override
    public EdgeTraversal<?, ?, M> divert(final SideEffectFunction<S> sideEffectFunction) {
        return (EdgeTraversal<?, ?, M>) super.divert(sideEffectFunction);
    }

    @Override
    public EdgeTraversal<?, ?, M> retain(final EdgeFrame... edges) {

        return (EdgeTraversal<?, ?, M>) super.retain(Arrays.asList(edges));
    }

    @Override
    public EdgeTraversal<?, ?, M> shuffle() {
        return (EdgeTraversal<?, ?, M>) super.shuffle();
    }

    @Override
    public EdgeTraversal<?, ?, M> except(final EdgeFrame... edges) {
        return (EdgeTraversal<?, ?, M>) super.retain(Arrays.asList(edges));
    }

    @Override
    public EdgeTraversal<?, ?, M> retain(final Iterable<?> collection) {

        return (EdgeTraversal<?, ?, M>) super.retain(Lists.newArrayList(collection));
    }

    @Override
    public EdgeTraversal<C, S, ? extends EdgeTraversal<C, S, M>> mark() {

        return (EdgeTraversal<C, S, ? extends EdgeTraversal<C, S, M>>) super.mark();
    }

    @Override
    public void removeAll() {
        getPipeline().removeAll();
    }

    @Override
    public <N> SplitTraversal<? extends Traversal<N, ?, ?, M>> copySplit(final TraversalFunction<EdgeFrame, ? extends Traversal<N, ?, ?, ?>>... traversals) {
        final Collection<Pipe> extractedPipes = Collections2.transform(Arrays.asList(traversals),
                                                                       new Function<TraversalFunction, Pipe>() {

                                                                           @Override
                                                                           public Pipe apply(final TraversalFunction input) {
                                                                               return ((AbstractTraversal) input.compute(new TEdge())).getPipeline();
                                                                           }
                                                                       });
        getPipeline().copySplit(extractedPipes.toArray(new Pipe[extractedPipes.size()]));
        return castToSplit();
    }

    @Override
    public EdgeTraversal<Tree<EdgeFrame>, Tree<EdgeFrame>, M> tree() {

        return (EdgeTraversal<Tree<EdgeFrame>, Tree<EdgeFrame>, M>) super.tree();
    }

    @Override
    public EdgeTraversal<?, ?, M> loop(final TraversalFunction<EdgeFrame, ? extends EdgeTraversal<?, ?, ?>> traversal) {
        final GremlinPipeline pipeline = ((AbstractTraversal) traversal.compute(new TEdge())).getPipeline();
        getPipeline().add(new LoopPipe(pipeline, LoopPipe.createTrueFunction(), null));

        return this;
    }

    @Override
    public EdgeTraversal<?, ?, M> loop(final TraversalFunction<EdgeFrame, ? extends EdgeTraversal<?, ?, ?>> traversal, final int depth) {
        final GremlinPipeline pipeline = ((AbstractTraversal) traversal.compute(new TEdge())).getPipeline();
        getPipeline().add(new LoopPipe(pipeline, LoopPipe.createLoopsFunction(depth), null));

        return this;
    }

    @Override
    public EdgeTraversal<?, ?, M> limit(final int limit) {
        return (EdgeTraversal<?, ?, M>) super.limit(limit);
    }
}
