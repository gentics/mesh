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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.TVertex;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.pipes.FermaGremlinPipeline;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Predicate;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.Tokens;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.transform.TransformPipe.Order;
import com.tinkerpop.pipes.util.structures.Pair;

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
@Deprecated
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
	public long count() {
		return getPipeline().count();
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

	@Override
	public Traversal<T, ?, ?, M> dedup(final TraversalFunction<T, ?> dedupFunction) {
		getPipeline().dedup(dedupFunction);
		return this;
	}

	@Override
	public Traversal<T, ?, ?, M> filter(final TraversalFunction<T, Boolean> filterFunction) {
		getPipeline().filter(new FramingTraversalFunction(filterFunction, graph()));
		return this;
	}
	
	@Override
	public Traversal<T, ?, ?, M> retain(final Iterable<?> collection) {
		getPipeline().retain(unwrap(Lists.newArrayList(collection)));
		return this;
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
	public void remove() {
		getPipeline().remove();
	}
}
