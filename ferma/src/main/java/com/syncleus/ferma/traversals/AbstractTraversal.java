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

import org.apache.commons.lang.NotImplementedException;

import com.google.common.collect.Sets;
import com.syncleus.ferma.pipes.DivertPipe;
import com.syncleus.ferma.pipes.TraversalFunctionPipe;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.Path;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.TVertex;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.pipes.*;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Predicate;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.Tokens;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.sideeffect.SideEffectPipe;
import com.tinkerpop.pipes.transform.TransformPipe.Order;
import com.tinkerpop.pipes.util.FluentUtility;
import com.tinkerpop.pipes.util.structures.Pair;
import com.tinkerpop.pipes.util.structures.Row;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.pipes.util.structures.Tree;

/**
 * The root traversal class. Wraps a Tinkerpop {@link GremlinPipeline}
 *
 * @param <T>
 *            The type of the objects coming off the pipe.
 * @param <C>
 *            The cap of the current pipe.
 * @param <S>
 *            The SideEffect of the current pipe.
 * @param <M>
 *            The current marked type for the current pipe.
 */
abstract class AbstractTraversal<T, C, S, M> implements Traversal<T, C, S, M> {
	private final FramedGraph graph;
	private final FermaGremlinPipeline pipeline;

	protected AbstractTraversal(final FramedGraph graph, final FermaGremlinPipeline pipeline) {
		this.graph = graph;
		this.pipeline = pipeline;
	}

	protected FramedGraph graph() {
		return this.graph;
	}

	protected FermaGremlinPipeline getPipeline() {
		return this.pipeline;
	}

	protected FramedGraph getGraph() {
		return graph;
	}

	@Override
	public VertexTraversal<?, ?, M> v() {
		getPipeline().V();
		return castToVertices();
	}

	@Override
	public EdgeTraversal<?, ?, M> e() {
		getPipeline().E();
		return castToEdges();
	}

	@Override
	public EdgeTraversal<?, ?, M> e(final Object... ids) {
		return (EdgeTraversal) graph().e(ids);
	}

	@Override
	public long count() {
		return getPipeline().count();
	}

	@Override
	public Traversal<T, ?, ?, M> as(final String name) {
		getPipeline().as(name);
		return this;
	}

	@Override
	public void iterate() {
		getPipeline().iterate();
	}

	protected Traversal<?, ?, ?, M> has(final String key) {
		getPipeline().has(key);
		return this;
	}

	protected Traversal<?, ?, ?, M> has(final String key, Object value) {
		if (value instanceof Enum)
			value = value.toString();
		getPipeline().has(key, value);
		return this;
	}

	protected Traversal<?, ?, ?, M> has(final String key, final Tokens.T compareToken, Object value) {
		if (value.getClass().isArray())
			value = Arrays.asList((Object[]) value);
		getPipeline().has(key, compareToken, value);
		return this;
	}

	protected Traversal<?, ?, ?, M> has(final String key, final Predicate predicate, Object value) {
		if (value instanceof Enum)
			value = value.toString();
		getPipeline().has(key, predicate, value);
		return this;
	}

	protected Traversal<?, ?, ?, M> hasNot(final String key) {
		getPipeline().hasNot(key);
		return this;
	}

	protected Traversal<?, ?, ?, M> hasNot(final String key, Object value) {
		if (value instanceof Enum)
			value = value.toString();
		getPipeline().hasNot(key, value);
		return this;
	}

	protected <Z> Traversal<T, ?, ?, M> interval(final String key, final Comparable<Z> startValue, final Comparable<Z> endValue) {
		Comparable pipelineStart = startValue;
		if (startValue instanceof Enum)
			pipelineStart = startValue.toString();

		Comparable pipelineEnd = endValue;
		if (endValue instanceof Enum)
			pipelineEnd = endValue.toString();

		getPipeline().interval(key, pipelineStart, pipelineEnd);
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> identity() {
		getPipeline()._();
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> except(final Iterable<?> collection) {
		getPipeline().except(unwrap(Lists.newArrayList(collection)));
		return this;
	}

	@Override
	public Traversal<Map<String, Object>, ?, ?, M> propertyMap(final String... keys) {
		getPipeline().map(keys);
		return castToTraversal();
	}

	@Override
	public Traversal<T, ?, ?, M> dedup() {
		getPipeline().dedup();
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> dedup(final TraversalFunction<T, ?> dedupFunction) {
		getPipeline().dedup(dedupFunction);
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> except(final String... namedSteps) {
		getPipeline().except(namedSteps);
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> filter(final TraversalFunction<T, Boolean> filterFunction) {
		getPipeline().filter(new FramingTraversalFunction(filterFunction, graph()));
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> random(final double bias) {
		getPipeline().random(bias);
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> range(final int low, final int high) {
		getPipeline().range(low, high);
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> retain(final Iterable<?> collection) {
		getPipeline().retain(unwrap(Lists.newArrayList(collection)));
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> retain(final String... namedSteps) {
		getPipeline().retain(namedSteps);
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> simplePath() {
		getPipeline().simplePath();
		return this;
	}

	@Override
	public Traversal<T, Collection<? extends T>, Collection<? extends T>, M> aggregate() {
		return this.aggregate(new ArrayList());

	}

	@Override
	public Traversal<T, Collection<? extends T>, Collection<? extends T>, M> aggregate(final Collection<? super T> aggregate) {
		getPipeline().aggregate(aggregate, new FramingTraversalFunction<>(graph()));
		return (Traversal<T, Collection<? extends T>, Collection<? extends T>, M>) this;
	}

	@Override
	public <N> Traversal<T, Collection<? extends N>, Collection<? extends N>, M> aggregate(final Collection<? super N> aggregate,
		final TraversalFunction<T, ? extends N> aggregateFunction) {
		getPipeline().aggregate(aggregate, new FramingTraversalFunction<>(aggregateFunction, graph()));
		return (Traversal<T, Collection<? extends N>, Collection<? extends N>, M>) this;
	}

	@Override
	public <N> Traversal<T, Collection<? extends N>, Collection<? extends N>, M> aggregate(
		final TraversalFunction<T, ? extends N> aggregateFunction) {
		getPipeline().aggregate(new FramingTraversalFunction<>(aggregateFunction, graph()));
		return (Traversal<T, Collection<? extends N>, Collection<? extends N>, M>) this;
	}

	@Override
	public <K, V> Traversal<T, Map<K, List<V>>, Map<K, List<V>>, M> groupBy(final Map<K, List<V>> map, final TraversalFunction<T, K> keyFunction,
		final TraversalFunction<T, Iterator<V>> valueFunction) {
		getPipeline().groupBy(map, new FramingTraversalFunction<>(keyFunction, graph()),
			new TraversalFunctionPipe(new FramingTraversalFunction<>(valueFunction, graph())));
		return (Traversal<T, Map<K, List<V>>, Map<K, List<V>>, M>) this;
	}

	@Override
	public <K, V> Traversal<T, Map<K, List<V>>, Map<K, List<V>>, M> groupBy(final TraversalFunction<T, K> keyFunction,
		final TraversalFunction<T, Iterator<V>> valueFunction) {

		getPipeline().groupBy(new FramingTraversalFunction<>(keyFunction, graph()),
			new TraversalFunctionPipe(new FramingTraversalFunction<>(valueFunction, graph())));
		return (Traversal<T, Map<K, List<V>>, Map<K, List<V>>, M>) this;
	}

	@Override
	public <K, V, V2> Traversal<T, Map<K, V2>, Map<K, V2>, M> groupBy(final Map<K, V2> reduceMap, final TraversalFunction<T, K> keyFunction,
		final TraversalFunction<T, Iterator<V>> valueFunction,
		final TraversalFunction<List<V>, V2> reduceFunction) {
		getPipeline().groupBy(reduceMap, new FramingTraversalFunction<>(keyFunction, graph()),
			new TraversalFunctionPipe(new FramingTraversalFunction<>(valueFunction, graph())), reduceFunction);
		return (Traversal<T, Map<K, V2>, Map<K, V2>, M>) this;
	}

	@Override
	public <K, V, V2> Traversal<T, Map<K, V2>, Map<K, V2>, M> groupBy(final TraversalFunction<T, K> keyFunction,
		final TraversalFunction<T, Iterator<V>> valueFunction,
		final TraversalFunction<List<V>, V2> reduceFunction) {
		getPipeline().groupBy(new FramingTraversalFunction<>(keyFunction, graph()),
			new TraversalFunctionPipe(new FramingTraversalFunction<>(valueFunction, graph())), reduceFunction);
		return (Traversal<T, Map<K, V2>, Map<K, V2>, M>) this;
	}

	@Override
	public <K> Traversal<T, Map<K, Long>, Map<K, Long>, M> groupCount(final Map<K, Long> map, final TraversalFunction<T, K> keyFunction,
		final TraversalFunction<Pair<T, Long>, Long> valueFunction) {
		getPipeline().groupCount(map, new FramingTraversalFunction<>(keyFunction, graph()),
			new TraversalFunctionPipe(new FramingTraversalFunction<>(valueFunction, graph())));
		return (Traversal<T, Map<K, Long>, Map<K, Long>, M>) this;
	}

	@Override
	public <K> Traversal<T, Map<K, Long>, Map<K, Long>, M> groupCount(final TraversalFunction<T, K> keyFunction,
		final TraversalFunction<Pair<T, Long>, Long> valueFunction) {
		getPipeline().groupCount(new FramingTraversalFunction<>(keyFunction, graph()),
			new FramingTraversalFunction<>(valueFunction, graph()));
		return (Traversal<T, Map<K, Long>, Map<K, Long>, M>) this;
	}

	@Override
	public <K> Traversal<T, Map<K, Long>, Map<K, Long>, M> groupCount(final Map<K, Long> map, final TraversalFunction<T, K> keyFunction) {
		getPipeline().groupCount(new FramingMap<>(map, graph()), new FramingTraversalFunction<>(keyFunction, graph()));
		return (Traversal<T, Map<K, Long>, Map<K, Long>, M>) this;
	}

	@Override
	public <K> Traversal<T, Map<K, Long>, Map<K, Long>, M> groupCount(final TraversalFunction<T, K> keyFunction) {
		getPipeline().groupCount(new FramingTraversalFunction<>(keyFunction, graph()));
		return (Traversal<T, Map<K, Long>, Map<K, Long>, M>) this;
	}

	@Override
	public Traversal<T, Map<T, Long>, Map<T, Long>, M> groupCount(final Map<T, Long> map) {
		getPipeline().groupCount(new FramingMap<>(map, graph()));
		return (Traversal<T, Map<T, Long>, Map<T, Long>, M>) this;
	}

	@Override
	public Traversal<T, Map<T, Long>, Map<T, Long>, M> groupCount() {
		return this.groupCount(new HashMap());

	}

	@Override
	public EdgeTraversal<?, ?, M> idEdge(final Graph graph) {
		getPipeline().idEdge(graph);
		return castToEdges();
	}

	@Override
	public <N> Traversal<N, ?, ?, M> id() {
		getPipeline().id();
		return castToTraversal();
	}

	@Override
	public <N> Traversal<? extends N, ?, ?, M> id(final Class<N> c) {
		return (Traversal<? extends N, ?, ?, M>) this.id();
	}

	@Override
	public VertexTraversal<?, ?, M> idVertex(final Graph graph) {
		getPipeline().idVertex(graph);
		return castToVertices();
	}

	@Override
	public Traversal<T, ?, ?, M> sideEffect(final SideEffectFunction<T> sideEffectFunction) {
		final FramingSideEffectFunction function = new FramingSideEffectFunction<>(sideEffectFunction, graph());
		getPipeline().sideEffect(new TraversalFunction() {

			@Override
			public Object compute(final Object argument) {
				function.execute(argument);
				return null;
			}

		});
		return this;
	}

	@Override
	public <N> Traversal<T, Collection<? extends N>, N, M> store(final Collection<? super N> storage) {
		getPipeline().store(storage, new FramingTraversalFunction<>(graph()));
		return (Traversal<T, Collection<? extends N>, N, M>) this;
	}

	@Override
	public <N> Traversal<T, Collection<? extends N>, N, M> store(final Collection<? super N> storage, final TraversalFunction<T, N> storageFunction) {
		getPipeline().store(storage, new FramingTraversalFunction<>(storageFunction, graph()));
		return (Traversal<T, Collection<? extends N>, N, M>) this;
	}

	@Override
	public Traversal<T, Collection<? extends T>, T, M> store() {
		return this.store(new ArrayList<T>());
	}

	@Override
	public <N> Traversal<T, Collection<? extends N>, N, M> store(final TraversalFunction<T, N> storageFunction) {
		getPipeline().store(new FramingTraversalFunction<>(storageFunction, graph()));
		return (Traversal<T, Collection<? extends N>, N, M>) this;
	}

	@Override
	public Traversal<T, Table, Table, M> table(final Table table, final Collection<String> stepNames,
		final TraversalFunction<?, ?>... columnFunctions) {
		getPipeline().table(table, stepNames, this.wrap(columnFunctions));
		return (Traversal<T, Table, Table, M>) this;
	}

	@Override
	public Traversal<T, Table, Table, M> table(final Table table, final TraversalFunction<?, ?>... columnFunctions) {
		getPipeline().table(table, wrap(columnFunctions));
		return (Traversal<T, Table, Table, M>) this;
	}

	@Override
	public Traversal<T, Table, Table, M> table(final TraversalFunction<?, ?>... columnFunctions) {
		getPipeline().table(wrap(columnFunctions));
		return (Traversal<T, Table, Table, M>) this;
	}

	@Override
	public Traversal<T, Table, Table, M> table(final Table table) {
		getPipeline().table(table, new FramingTraversalFunction<>(graph()));
		return (Traversal<T, Table, Table, M>) this;
	}

	@Override
	public Traversal<T, Table, Table, M> table() {
		getPipeline().table(new FramingTraversalFunction<>(graph()));
		return (Traversal<T, Table, Table, M>) this;
	}

	@Override
	public <N> Traversal<T, Tree<N>, Tree<N>, M> tree(final Tree<N> tree) {
		getPipeline().tree(tree, new FramingTraversalFunction<>(graph()));
		return (Traversal<T, Tree<N>, Tree<N>, M>) this;
	}

	@Override
	public Traversal<T, Tree<T>, Tree<T>, M> tree() {
		getPipeline().tree(new FramingTraversalFunction<>(graph()));
		return (Traversal<T, Tree<T>, Tree<T>, M>) this;
	}

	@Override
	public Traversal<T, ?, ?, M> memoize(final String namedStep) {
		getPipeline().memoize(namedStep);
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> memoize(final String namedStep, final Map<?, ?> map) {
		getPipeline().memoize(namedStep, map);
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> order() {
		getPipeline().order();
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> order(final Order order) {
		getPipeline().order(order);
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> order(final Tokens.T order) {
		getPipeline().order(order);
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> order(final Comparator<? super T> compareFunction) {
		final FramingComparator framingComparator = new FramingComparator(compareFunction, graph());
		getPipeline().order(new TraversalFunction<Pair<Object, Object>, Integer>() {

			@Override
			public Integer compute(final Pair<Object, Object> argument) {
				return framingComparator.compare(argument.getA(), argument.getB());
			}
		});
		return this;
	}

	@Override
	public Traversal<Path, ?, ?, M> path(final TraversalFunction<?, ?>... pathFunctions) {
		if (pathFunctions.length == 0)
			getPipeline().path(new FramingTraversalFunction<>(graph()));
		else
			getPipeline().path(wrap(pathFunctions));
		return castToTraversal();
	}

	@Override
	public Traversal<Row<?>, ?, ?, M> select(final Collection<String> stepNames, final TraversalFunction<?, ?>... columnFunctions) {
		getPipeline().select(stepNames, wrap(columnFunctions));
		return castToTraversal();
	}

	@Override
	public Traversal<Row<?>, ?, ?, M> select(final TraversalFunction<?, ?>... columnFunctions) {
		getPipeline().select(wrap(columnFunctions));
		return castToTraversal();
	}

	@Override
	public Traversal<Row<?>, ?, ?, M> select() {
		getPipeline().select(new FramingTraversalFunction<>(graph()));
		return castToTraversal();
	}

	@Override
	public Traversal<T, ?, ?, M> shuffle() {
		getPipeline().shuffle();
		return this;
	}

	@Override
	public C cap() {
		getPipeline().cap();
		Object next = castToTraversal().next();
		if (next instanceof FramingMap)
			next = ((FramingMap) next).getDelegate();
		if (next instanceof FramingCollection)
			next = ((FramingCollection) next).getDelegate();
		return (C) next;
	}

	@Override
	public Traversal<T, ?, ?, M> divert(final SideEffectFunction<S> sideEffectFunction) {

		final FramingSideEffectFunction framingSideEffectFunction = new FramingSideEffectFunction(sideEffectFunction, graph());
		getPipeline().add(
			new DivertPipe((SideEffectPipe) FluentUtility.removePreviousPipes(getPipeline(), 1).get(0), new TraversalFunction() {

				@Override
				public Object compute(final Object argument) {
					framingSideEffectFunction.execute(argument);
					return null;
				}

			}));
		return this;
	}

	@Override
	public <N> Traversal<? extends N, ?, ?, M> transform(final TraversalFunction<T, N> function) {
		getPipeline().transform(new FramingTraversalFunction(function, graph()));
		return castToTraversal();
	}

	@Override
	public <N> Traversal<N, ?, ?, M> start(final N object) {
		getPipeline().start(object);
		return (Traversal<N, ?, ?, M>) this;
	}

	@Override
	public List<? extends T> next(final int number) {
		return Lists.transform(getPipeline().next(number), new Function() {

			@Override
			public Object apply(final Object input) {
				if (input instanceof Edge)
					return graph().frameElement((Element) input, TEdge.class);
				else if (input instanceof Vertex)
					return graph().frameElement((Element) input, TVertex.class);
				return input;
			}
		});

	}

	@Override
	public List<? extends T> toList() {

		return Lists.transform(getPipeline().toList(), new Function() {

			@Override
			public Object apply(final Object input) {
				if (input instanceof Edge)
					return graph().frameElementExplicit((Element) input, TEdge.class);
				else if (input instanceof Vertex)
					return graph().frameElementExplicit((Element) input, TVertex.class);
				return input;
			}
		});

	}

	@Override
	public Set<? extends T> toSet() {

		return Sets.newHashSet(toList());

	}

	@Override
	public Collection<? extends T> fill(final Collection<? super T> collection) {
		return getPipeline().fill(new FramingCollection<>(collection, graph()));
	}

	@Override
	public Traversal<T, C, S, M> enablePath() {
		getPipeline().enablePath();
		return this;
	}

	@Override
	public Traversal<T, C, S, M> optimize(final boolean optimize) {
		getPipeline().optimize(optimize);
		return this;
	}

	@Override
	public T next() {
		final Object e = getPipeline().next();
		if (e instanceof Edge)
			return (T) graph().frameElementExplicit((Element) e, TEdge.class);
		else if (e instanceof Vertex)
			return (T) graph().frameElementExplicit((Element) e, TVertex.class);
		return (T) e;
	}

	@Override
	public T nextOrDefault(final T defaultValue) {
		if (getPipeline().hasNext())
			return next();
		else
			return defaultValue;
	}

	@Override
	public EdgeTraversal<?, ?, M> start(final EdgeFrame object) {
		getPipeline().start(object);
		return castToEdges();
	}

	@Override
	public VertexTraversal<?, ?, M> start(final VertexFrame object) {
		getPipeline().start(object);
		return castToVertices();
	}

	@Override
	public <N> Traversal<N, ?, ?, M> property(final String key) {
		getPipeline().property(key);
		return castToTraversal();
	}

	@Override
	public <N> Traversal<? extends N, ?, ?, M> property(final String key, final Class<N> type) {
		return property(key);

	}

	protected abstract <W, X, Y, Z> Traversal<W, X, Y, Z> castToTraversal();

	@Override
	public boolean hasNext() {
		return getPipeline().hasNext();
	}

	@Override
	public Iterator<T> iterator() {
		return Iterators.transform(getPipeline(), new Function() {

			@Override
			public Object apply(final Object input) {
				if (input instanceof Element)
					return graph().frameElement((Element) input, TVertex.class);

				return input;
			}
		});
	}

	private HashSet<? extends Element> unwrap(final Collection<?> collection) {
		final HashSet unwrapped = new HashSet(Collections2.transform(collection, new Function<Object, Object>() {

			@Override
			public Object apply(final Object input) {
				if (input instanceof VertexFrame)
					return ((VertexFrame) input).getElement();
				if (input instanceof EdgeFrame)
					return ((EdgeFrame) input).getElement();
				return input;
			}
		}));
		return unwrapped;
	}

	private <X, Y, Z> TraversalFunction<? extends Z, ? extends Y>[] wrap(final TraversalFunction<? extends X, ? extends Y>... branchFunctions) {
		final Collection<TraversalFunction<Z, Y>> wrapped = Collections2.transform(Arrays.asList(branchFunctions),
			new Function<TraversalFunction<? extends X, ? extends Y>, TraversalFunction<Z, Y>>() {

				@Override
				public TraversalFunction<Z, Y> apply(final TraversalFunction<? extends X, ? extends Y> input) {
					return new FramingTraversalFunction(input, graph());
				}
			});
		final TraversalFunction<Z, Y>[] wrappedArray = wrapped.toArray(new TraversalFunction[wrapped.size()]);
		return wrappedArray;
	}

	@Override
	public Traversal<T, ?, ?, M> gatherScatter() {
		getPipeline().gather().scatter();
		return this;
	}

	@Override
	public Traversal<T, C, S, ? extends Traversal<T, C, S, M>> mark() {
		final MarkId mark = pushMark();
		getPipeline().as(mark.id);

		return (Traversal<T, C, S, ? extends Traversal<T, C, S, M>>) this;
	}

	@Override
	public M back() {
		final MarkId mark = popMark();
		getPipeline().back(mark.id);
		return (M) mark.traversal;
	}

	@Override
	public M optional() {
		final MarkId mark = popMark();
		getPipeline().optional(mark.id);
		return (M) mark.traversal;
	}

	/**
	 * Cast the traversal as a split traversal
	 * 
	 * @return
	 */
	protected abstract <N> SplitTraversal<N> castToSplit();

	/**
	 * Cast the traversal as a vertex traversal
	 * 
	 * @return
	 */
	protected abstract VertexTraversal<C, S, M> castToVertices();

	/**
	 * Cast the traversal to an edge traversalT
	 * 
	 * @return
	 */
	protected abstract EdgeTraversal<C, S, M> castToEdges();

	protected abstract <W, X, Y, Z> MarkId<W, X, Y, Z> pushMark();

	protected abstract <W, X, Y, Z> MarkId<W, X, Y, Z> popMark();

	protected static class MarkId<T, C, S, M> {

		Traversal<T, C, S, M> traversal;
		String id;
	}

	@Override
	public VertexTraversal<?, ?, M> v(final Collection<?> ids) {
		throw new NotImplementedException("Method not implemented");
	}

	@Override
	public VertexTraversal<?, ?, M> v(final String key, final Object value) {
		getPipeline().V(key, value);
		return castToVertices();
	}

	@Override
	public EdgeTraversal<?, ?, M> e(final Collection<?> ids) {
		return (EdgeTraversal) graph().e(ids);
	}

	@Override
	public Traversal<T, ?, ?, M> limit(final int limit) {
		return range(0, limit - 1);
	}

	@Override
	public void remove() {
		getPipeline().remove();
	}
}
