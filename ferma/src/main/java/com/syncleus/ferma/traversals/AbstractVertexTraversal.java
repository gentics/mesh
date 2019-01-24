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
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.syncleus.ferma.*;
import com.syncleus.ferma.pipes.FermaGremlinPipeline;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Predicate;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.Tokens.T;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.branch.LoopPipe;
import com.tinkerpop.pipes.transform.TransformPipe.Order;
import com.tinkerpop.pipes.util.structures.Pair;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.pipes.util.structures.Tree;

/**
 * Vertex specific traversal. This class is abstract and as such is never instantiated directly.
 *
 * @param <C> The cap of the current pipe.
 * @param <S> The SideEffect of the current pipe.
 * @param <M> The current marked type for the current pipe.
 */
abstract class AbstractVertexTraversal<C, S, M> extends AbstractTraversal<VertexFrame, C, S, M> implements VertexTraversal<C, S, M> {
    protected AbstractVertexTraversal(FramedGraph graph, FermaGremlinPipeline pipeline) {
        super(graph, pipeline);
    }

    @Override
    public VertexFrame next() {
        return graph().frameElement((Vertex) getPipeline().next(), VertexFrame.class);
    }

    @Override
    public VertexFrame nextOrAdd() {

        return nextOrAdd(TVertex.DEFAULT_INITIALIZER);
    }

    @Override
    public <N> Iterable<? extends N> frameExplicit(final Class<N> kind) {
        return Iterables.transform(getPipeline(), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElementExplicit((Element) input, kind);
            }
        });
    }

    @Override
    public VertexTraversal<Table, Table, M> table() {
        return (VertexTraversal<Table, Table, M>) super.table();
    }

    @Override
    public VertexTraversal<Table, Table, M> table(final TraversalFunction... columnFunctions) {

        return (VertexTraversal<Table, Table, M>) super.table(columnFunctions);
    }

    @Override
    public VertexTraversal<Table, Table, M> table(final Table table) {

        return (VertexTraversal<Table, Table, M>) super.table(table);
    }

    @Override
    public VertexTraversal<Table, Table, M> table(final Table table, final Collection stepNames, final TraversalFunction... columnFunctions) {

        return (VertexTraversal<Table, Table, M>) super.table(table, stepNames, columnFunctions);
    }

    @Override
    public VertexTraversal<Table, Table, M> table(final Table table, final TraversalFunction<?, ?>... columnFunctions) {
        return (VertexTraversal) super.table(table, columnFunctions);
    }

    @Override
    public <N> VertexTraversal<Tree<N>, Tree<N>, M> tree(final Tree<N> tree) {
        return (VertexTraversal) super.tree(tree);
    }

    @Override
    public Traversal<VertexFrame, Collection<? extends VertexFrame>, VertexFrame, M> store() {
        return super.store(new FramingTraversalFunction(graph(), VertexFrame.class));
    }

    @Override
    public <N> Traversal<VertexFrame, Collection<? extends N>, N, M> store(final Collection<? super N> storage) {
        return super.store(storage);
    }

    @Override
    public <N> Traversal<VertexFrame, Collection<? extends N>, N, M> store(final Collection<? super N> storage,
                                                                           final TraversalFunction<VertexFrame, N> storageFunction) {
        return super.store(storage);
    }

    @Override
    public <N> Traversal<VertexFrame, Collection<? extends N>, N, M> store(final TraversalFunction<VertexFrame, N> storageFunction) {
        return (VertexTraversal) super.store(storageFunction);
    }

    @Override
    public VertexTraversal<Map<VertexFrame, Long>, Map<VertexFrame, Long>, M> groupCount() {
        return (VertexTraversal) super.groupCount();
    }

    @Override
    public VertexTraversal<Map<VertexFrame, Long>, Map<VertexFrame, Long>, M> groupCount(final Map<VertexFrame, Long> map) {
        return (VertexTraversal<Map<VertexFrame, Long>, Map<VertexFrame, Long>, M>) super.groupCount(map);
    }

    @Override
    public <K> VertexTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(final Map<K, Long> map, final TraversalFunction<VertexFrame, K> keyFunction) {
        return (VertexTraversal<Map<K, Long>, Map<K, Long>, M>) super.groupCount(map, keyFunction);
    }

    @Override
    public <K> VertexTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(final Map<K, Long> map, final TraversalFunction<VertexFrame, K> keyFunction,
                                                                         final TraversalFunction<Pair<VertexFrame, Long>, Long> valueFunction) {
        return (VertexTraversal<Map<K, Long>, Map<K, Long>, M>) super.groupCount(map, keyFunction, valueFunction);
    }

    @Override
    public <K> VertexTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(final TraversalFunction<VertexFrame, K> keyFunction) {
        return (VertexTraversal<Map<K, Long>, Map<K, Long>, M>) super.groupCount(keyFunction);
    }

    @Override
    public <K> VertexTraversal<Map<K, Long>, Map<K, Long>, M> groupCount(final TraversalFunction<VertexFrame, K> keyFunction, final TraversalFunction<Pair<VertexFrame, Long>, Long> valueFunction) {
        return (VertexTraversal<Map<K, Long>, Map<K, Long>, M>) super.groupCount(keyFunction, valueFunction);
    }

    @Override
    public <K, V, V2> VertexTraversal<Map<K, V2>, Map<K, V2>, M> groupBy(final TraversalFunction<VertexFrame, K> keyFunction, final TraversalFunction<VertexFrame, Iterator<V>> valueFunction,
                                                                         final TraversalFunction<List<V>, V2> reduceFunction) {
        return (VertexTraversal<Map<K, V2>, Map<K, V2>, M>) super.groupBy(keyFunction, valueFunction, reduceFunction);
    }

    @Override
    public <K, V> VertexTraversal<Map<K, List<V>>, Map<K, List<V>>, M> groupBy(final TraversalFunction<VertexFrame, K> keyFunction, final TraversalFunction<VertexFrame, Iterator<V>> valueFunction) {

        return (VertexTraversal<Map<K, List<V>>, Map<K, List<V>>, M>) super.groupBy(keyFunction, valueFunction);
    }

    @Override
    public <K, V, V2> VertexTraversal<Map<K, V2>, Map<K, V2>, M> groupBy(final Map<K, V2> reduceMap, final TraversalFunction<VertexFrame, K> keyFunction,
                                                                         final TraversalFunction<VertexFrame, Iterator<V>> valueFunction, final TraversalFunction<List<V>, V2> reduceFunction) {
        return (VertexTraversal<Map<K, V2>, Map<K, V2>, M>) super.groupBy(reduceMap, keyFunction, valueFunction, reduceFunction);
    }

    @Override
    public <K, V> VertexTraversal<Map<K, List<V>>, Map<K, List<V>>, M> groupBy(final Map<K, List<V>> map, final TraversalFunction<VertexFrame, K> keyFunction,
                                                                               final TraversalFunction<VertexFrame, Iterator<V>> valueFunction) {
        return (VertexTraversal<Map<K, List<V>>, Map<K, List<V>>, M>) super.groupBy(map, keyFunction, valueFunction);
    }

    @Override
    public VertexTraversal<?, ?, M> filter(final TraversalFunction<VertexFrame, Boolean> filterFunction) {
        return (VertexTraversal<?, ?, M>) super.filter(filterFunction);

    }

    @Override
    public VertexTraversal<Collection<? extends VertexFrame>, Collection<? extends VertexFrame>, M> aggregate() {
        return (VertexTraversal<Collection<? extends VertexFrame>, Collection<? extends VertexFrame>, M>) super.aggregate();

    }

    @Override
    public VertexTraversal<Collection<? extends VertexFrame>, Collection<? extends VertexFrame>, M> aggregate(final Collection<? super VertexFrame> aggregate) {
        return (VertexTraversal<Collection<? extends VertexFrame>, Collection<? extends VertexFrame>, M>) super.aggregate(aggregate);

    }

    @Override
    public <N> VertexTraversal<Collection<? extends N>, Collection<? extends N>, M> aggregate(final Collection<? super N> aggregate, final TraversalFunction<VertexFrame, ? extends N> aggregateFunction) {
        Traversal<VertexFrame, ?, ?, M> traversal = super.aggregate(aggregate, aggregateFunction);
        return (VertexTraversal<Collection<? extends N>, Collection<? extends N>, M>) traversal;

    }

    @Override
    public <N> VertexTraversal<Collection<? extends N>, Collection<? extends N>, M> aggregate(final TraversalFunction<VertexFrame, ? extends N> aggregateFunction) {
        Traversal<VertexFrame, ?, ?, M> traversal = super.aggregate(aggregateFunction);
        return (VertexTraversal<Collection<? extends N>, Collection<? extends N>, M>) traversal;

    }

    @Override
    public VertexTraversal<?, ?, M> sideEffect(final SideEffectFunction<VertexFrame> sideEffectFunction) {
        return (VertexTraversal<?, ?, M>) super.sideEffect(sideEffectFunction);
    }

    @Override
    public VertexTraversal<?, ?, M> random(final double bias) {

        return (VertexTraversal<?, ?, M>) super.random(bias);
    }

    @Override
    public VertexTraversal<?, ?, M> dedup(final TraversalFunction<VertexFrame, ?> dedupFunction) {
        return (VertexTraversal) super.dedup(dedupFunction);
    }

    @Override
    public VertexTraversal<?, ?, M> except(final String... namedSteps) {

        return (VertexTraversal<?, ?, M>) super.except(namedSteps);
    }

    @Override
    public VertexTraversal<?, ?, M> except(final Iterable<?> collection) {
        return (VertexTraversal<?, ?, M>) super.except(Lists.newArrayList(collection));
    }

    @Override
    public VertexTraversal<?, ?, M> range(final int low, final int high) {
        return (VertexTraversal<?, ?, M>) super.range(low, high);
    }

    @Override
    public VertexTraversal<?, ?, M> and(final TraversalFunction<VertexFrame, Traversal<?, ?, ?, ?>>... traversals) {
        final Collection<Pipe> extractedPipes = Collections2.transform(Arrays.asList(traversals), new Function<TraversalFunction, Pipe>() {

            @Override
            public Pipe apply(final TraversalFunction input) {
                return ((AbstractTraversal) input.compute(new TVertex())).getPipeline();
            }
        });
        getPipeline().and(extractedPipes.toArray(new Pipe[extractedPipes.size()]));
        return this;
    }

    @Override
    public VertexTraversal<?, ?, M> or(final TraversalFunction<VertexFrame, Traversal<?, ?, ?, ?>>... traversals) {
        final Collection<Pipe> extractedPipes = Collections2.transform(Arrays.asList(traversals), new Function<TraversalFunction, Pipe>() {

            @Override
            public Pipe apply(final TraversalFunction input) {
                return ((AbstractTraversal) input.compute(new TVertex())).getPipeline();
            }
        });
        getPipeline().or(extractedPipes.toArray(new Pipe[extractedPipes.size()]));
        return this;
    }

    @Override
    public VertexTraversal<?, ?, M> order() {
        return (VertexTraversal<?, ?, M>) super.order();
    }

    @Override
    public VertexTraversal<?, ?, M> order(final Order order) {
        return (VertexTraversal<?, ?, M>) super.order(order);
    }

    @Override
    public VertexTraversal<?, ?, M> order(final Comparator<? super VertexFrame> compareFunction) {
        return (VertexTraversal<?, ?, M>) super.order(compareFunction);
    }

    @Override
    public VertexTraversal<?, ?, M> order(final T order) {
        return (VertexTraversal<?, ?, M>) super.order(order);
    }

    @Override
    public VertexTraversal<?, ?, M> dedup() {

        return (VertexTraversal<?, ?, M>) super.dedup();
    }

    @Override
    public VertexTraversal<?, ?, M> retain(final String... namedSteps) {
        return (VertexTraversal<?, ?, M>) super.retain(namedSteps);
    }

    @Override
    public VertexTraversal<?, ?, M> simplePath() {
        return (VertexTraversal<?, ?, M>) super.simplePath();
    }

    @Override
    public VertexTraversal<?, ?, M> memoize(final String namedStep) {
        return (VertexTraversal<?, ?, M>) super.memoize(namedStep);

    }

    @Override
    public VertexTraversal<?, ?, M> memoize(final String namedStep, final Map<?, ?> map) {
        return (VertexTraversal<?, ?, M>) super.memoize(namedStep, map);

    }

    @Override
    public VertexTraversal<?, ?, M> out(final int branchFactor, final String... labels) {
        getPipeline().out(branchFactor, labels);
        return this;
    }

    @Override
    public VertexTraversal<?, ?, M> out(final String... labels) {
        getPipeline().out(labels);
        return this;
    }

    @Override
    public VertexTraversal<?, ?, M> in(final int branchFactor, final String... labels) {
        getPipeline().in(branchFactor, labels);
        return this;
    }

    @Override
    public VertexTraversal<?, ?, M> in(final String... labels) {
        getPipeline().in(labels);
        return this;
    }

    @Override
    public VertexTraversal<?, ?, M> both(final int branchFactor, final String... labels) {
        getPipeline().both(branchFactor, labels);
        return this;
    }

    @Override
    public VertexTraversal<?, ?, M> both(final String... labels) {
        getPipeline().both(labels);
        return this;
    }

    @Override
    public EdgeTraversal<?, ?, M> outE(final int branchFactor, final String... labels) {
        getPipeline().outE(branchFactor, labels);
        return castToEdges();
    }

    @Override
    public EdgeTraversal<?, ?, M> outE(final String... labels) {
        getPipeline().outE(labels);
        return castToEdges();
    }

    @Override
    public EdgeTraversal<?, ?, M> inE(final int branchFactor, final String... labels) {
        getPipeline().inE(branchFactor, labels);
        return castToEdges();
    }

    @Override
    public EdgeTraversal<?, ?, M> inE(final String... labels) {
        getPipeline().inE(labels);
        return castToEdges();
    }

    @Override
    public EdgeTraversal<?, ?, M> bothE(final int branchFactor, final String... labels) {
        getPipeline().bothE(branchFactor, labels);
        return castToEdges();
    }

    @Override
    public EdgeTraversal<?, ?, M> bothE(final String... labels) {
        getPipeline().bothE(labels);
        return castToEdges();
    }

    @Override
    public <Z> VertexTraversal<?, ?, M> interval(final String key, final Comparable<Z> startValue, final Comparable<Z> endValue) {
        return (VertexTraversal<?, ?, M>) super.interval(key, startValue, endValue);
    }

    @Override
    public <N> N next(final Class<N> kind) {
        return graph().frameElement((Element) getPipeline().next(), kind);
    }

    @Override
    public <N> N nextExplicit(final Class<N> kind) {
        return graph().frameElementExplicit((Element) getPipeline().next(), kind);
    }

    @Override
    public <N> N nextOrDefault(final Class<N> kind, final N defaultValue) {
        if (getPipeline().hasNext())
            return next(kind);
        else
            return defaultValue;
    }

    @Override
    public <N> N nextOrDefaultExplicit(final Class<N> kind, final N defaultValue) {
        if (getPipeline().hasNext())
            return nextExplicit(kind);
        else
            return defaultValue;
    }

    @Override
    public <N> N nextOrAdd(final ClassInitializer<N> initializer) {
        try {
            return graph().frameElement((Element) getPipeline().next(), initializer.getInitializationType());
        }
        catch (final NoSuchElementException e) {
            return graph().addFramedVertex(null, initializer);
        }
    }
    
    @Override
    public <N> N nextOrAdd(final Class<N> kind) {
        return this.nextOrAdd(new DefaultClassInitializer<>(kind));
    }

    @Override
    public <N> N nextOrAddExplicit(final ClassInitializer<N> initializer) {
        try {
            return graph().frameElementExplicit((Element) getPipeline().next(), initializer.getInitializationType());
        }
        catch (final NoSuchElementException e) {
            return graph().addFramedVertex(null, initializer);
        }
    }
    
    @Override
    public <N> N nextOrAddExplicit(final Class<N> kind) {
        return this.nextOrAddExplicit(new DefaultClassInitializer<>(kind));
    }

    @Override
    public <N> List<? extends N> next(final int amount, final Class<N> kind) {
        return Lists.transform(getPipeline().next(amount), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElement((Element) input, kind);
            }
        });
    }

    @Override
    public <N> List<? extends N> nextExplicit(final int amount, final Class<N> kind) {
        return Lists.transform(getPipeline().next(amount), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElementExplicit((Element) input, kind);
            }
        });
    }

    @Override
    public <N> Iterable<N> frame(final Class<N> kind) {
        return Iterables.transform(getPipeline(), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElement((Element) input, kind);
            }
        });
    }

    @Override
    public <N> List<? extends N> toList(final Class<N> kind) {
        return Lists.transform(getPipeline().toList(), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElement((Element) input, kind);
            }
        });
    }

    @Override
    public <N> List<? extends N> toListExplicit(final Class<N> kind) {
        return Lists.transform(getPipeline().toList(), new Function() {

            @Override
            public Object apply(final Object input) {
                return graph().frameElementExplicit((Element) input, kind);
            }
        });
    }

    @Override
    public <N> Set<? extends N> toSet(final Class<N> kind) {
        return Sets.newHashSet(toList(kind));
    }

    @Override
    public <N> Set<? extends N> toSetExplicit(final Class<N> kind) {
        return Sets.newHashSet(toListExplicit(kind));
    }

    @Override
    public List<? extends VertexFrame> toList() {
        return toListExplicit(VertexFrame.class);
    }

    @Override
    public Set<? extends VertexFrame> toSet() {
        return toSetExplicit(VertexFrame.class);
    }

    @Override
    public VertexTraversal<?, ?, M> has(final String key) {
        return (VertexTraversal<?, ?, M>) super.has(key);
    }

    @Override
    public VertexTraversal has(final String key, final Object value) {
        return (VertexTraversal) super.has(key, value);
    }

    @Override
    public VertexTraversal<?, ?, M> has(final String key, final Predicate predicate, final Object value) {
        return (VertexTraversal<?, ?, M>) super.has(key, predicate, value);
    }

    @Override
    public VertexTraversal<?, ?, M> has(final String key, final com.tinkerpop.gremlin.Tokens.T compareToken, final Object value) {
        return (VertexTraversal<?, ?, M>) super.has(key, compareToken, value);
    }

    @Override
    public VertexTraversal<?, ?, M> has(Class<?> clazz) {
        graph().getTypeResolver().hasType(this, clazz);
        return this;
    }

    @Override
    public VertexTraversal<?, ?, M> hasNot(final String key) {
        return (VertexTraversal<?, ?, M>) super.hasNot(key);
    }

    @Override
    public VertexTraversal<?, ?, M> hasNot(final String key, final Object value) {
        return (VertexTraversal<?, ?, M>) super.hasNot(key, value);
    }

    @Override
    public VertexTraversal<?, ?, M> hasNot(Class<?> clazz) {
        graph().getTypeResolver().hasNotType(this, clazz);
        return this;
    }

    @Override
    public VertexTraversal<?, ?, M> as(final String name) {
        return (VertexTraversal<?, ?, M>) super.as(name);
    }

    @Override
    public VertexTraversal<?, ?, M> identity() {
        return (VertexTraversal<?, ?, M>) super.identity();
    }

    @Override
    public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkOut(final String label, final String namedStep) {
        getPipeline().linkOut(label, namedStep);
        return (VertexTraversal<List<EdgeFrame>, EdgeFrame, M>) this;
    }

    @Override
    public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkIn(final String label, final String namedStep) {
        getPipeline().linkIn(label, namedStep);
        return (VertexTraversal<List<EdgeFrame>, EdgeFrame, M>) this;
    }

    @Override
    public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkBoth(final String label, final String namedStep) {
        getPipeline().linkBoth(label, namedStep);
        return (VertexTraversal<List<EdgeFrame>, EdgeFrame, M>) this;
    }

    @Override
    public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkOut(final String label, final Vertex other) {
        getPipeline().linkOut(label, other);
        return (VertexTraversal<List<EdgeFrame>, EdgeFrame, M>) this;
    }

    @Override
    public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkIn(final String label, final Vertex other) {
        getPipeline().linkIn(label, other);
        return (VertexTraversal<List<EdgeFrame>, EdgeFrame, M>) this;
    }

    @Override
    public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkBoth(final String label, final Vertex other) {
        getPipeline().linkBoth(label, other);
        return (VertexTraversal<List<EdgeFrame>, EdgeFrame, M>) this;
    }

    @Override
    public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkOut(final String label, final VertexFrame other) {
        getPipeline().linkOut(label, other.getElement());
        return (VertexTraversal<List<EdgeFrame>, EdgeFrame, M>) this;
    }

    @Override
    public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkIn(final String label, final VertexFrame other) {
        getPipeline().linkIn(label, other.getElement());
        return (VertexTraversal<List<EdgeFrame>, EdgeFrame, M>) this;
    }

    @Override
    public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkBoth(final String label, final VertexFrame other) {
        getPipeline().linkBoth(label, other.getElement());
        return (VertexTraversal<List<EdgeFrame>, EdgeFrame, M>) this;
    }

    @Override
    public <N> Collection<? extends N> fill(final Collection<? super N> collection, final Class<N> kind) {

        return getPipeline().fill(new FramingCollection<>(collection, graph(), kind));
    }

    @Override
    public <N> Collection<? extends N> fillExplicit(final Collection<? super N> collection, final Class<N> kind) {

        return getPipeline().fill(new FramingCollection<>(collection, graph(), kind, true));
    }

    @Override
    public VertexTraversal<?, ?, M> gatherScatter() {
        return (VertexTraversal<?, ?, M>) super.gatherScatter();
    }

    @Override
    public VertexTraversal<?, ?, M> divert(final SideEffectFunction<S> sideEffectFunction) {
        return (VertexTraversal<?, ?, M>) super.divert(sideEffectFunction);
    }

    @Override
    public VertexTraversal<?, ?, M> retain(final VertexFrame... vertices) {

        return (VertexTraversal<?, ?, M>) super.retain(Arrays.asList(vertices));
    }

    @Override
    public VertexTraversal<?, ?, M> except(final VertexFrame... vertices) {

        return (VertexTraversal<?, ?, M>) super.except(Arrays.asList(vertices));
    }

    @Override
    public VertexTraversal<?, ?, M> shuffle() {

        return (VertexTraversal<?, ?, M>) super.shuffle();
    }

    @Override
    public VertexTraversal<?, ?, M> retain(final Iterable<?> collection) {
        return (VertexTraversal<?, ?, M>) super.retain(Lists.newArrayList(collection));
    }

    @Override
    public VertexTraversal<C, S, ? extends VertexTraversal<C, S, M>> mark() {

        return (VertexTraversal<C, S, VertexTraversal<C, S, M>>) super.mark();
    }

    @Override
    public void removeAll() {
        getPipeline().removeAll();
    }

    @Override
    public <N> SplitTraversal<? extends Traversal<N, ?, ?, M>> copySplit(final TraversalFunction<VertexFrame, ? extends Traversal<N, ?, ?, ?>>... traversals) {
        final Collection<Pipe> extractedPipes = Collections2.transform(Arrays.asList(traversals),
                                                                       new Function<TraversalFunction, Pipe>() {

                                                                           @Override
                                                                           public Pipe apply(final TraversalFunction input) {
                                                                               return ((AbstractTraversal) input.compute(new TVertex())).getPipeline();
                                                                           }
                                                                       });
        getPipeline().copySplit(extractedPipes.toArray(new Pipe[extractedPipes.size()]));
        return castToSplit();
    }

    @Override
    public VertexTraversal<Tree<VertexFrame>, Tree<VertexFrame>, M> tree() {

        return (VertexTraversal<Tree<VertexFrame>, Tree<VertexFrame>, M>) super.tree();
    }

    @Override
    public VertexTraversal<?, ?, M> loop(final TraversalFunction<VertexFrame, ? extends VertexTraversal<?, ?, ?>> traversal) {
        final GremlinPipeline pipeline = ((AbstractTraversal) traversal.compute(new TVertex())).getPipeline();
        getPipeline().add(new LoopPipe(pipeline, LoopPipe.createTrueFunction(), null));

        return this;
    }

    @Override
    public VertexTraversal<?, ?, M> loop(final TraversalFunction<VertexFrame, ? extends VertexTraversal<?, ?, ?>> traversal, final int depth) {
        final GremlinPipeline pipeline = ((AbstractTraversal) traversal.compute(new TVertex())).getPipeline();
        getPipeline().add(new LoopPipe(pipeline, LoopPipe.createLoopsFunction(depth), null));

        return this;
    }

    @Override
    public VertexTraversal<?, ?, M> limit(final int limit) {
        return (VertexTraversal) super.limit(limit);
    }
}
