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
 * @param <C>
 *            The cap of the current pipe.
 * @param <S>
 *            The SideEffect of the current pipe.
 * @param <M>
 *            The current marked type for the current pipe.
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
	public EdgeTraversal<?, ?, M> dedup(final TraversalFunction<EdgeFrame, ?> dedupFunction) {
		return (EdgeTraversal<?, ?, M>) super.dedup(dedupFunction);
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
	public java.util.Iterator<EdgeFrame> iterator() {
		return Iterators.transform(getPipeline(), new Function() {

			@Override
			public Object apply(final Object input) {
				return graph().frameElement((Element) input, EdgeFrame.class);
			}
		});
	}

	@Override
	public EdgeTraversal<?, ?, M> retain(final EdgeFrame... edges) {

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
	public <N> SplitTraversal<? extends Traversal<N, ?, ?, M>> copySplit(
		final TraversalFunction<EdgeFrame, ? extends Traversal<N, ?, ?, ?>>... traversals) {
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

}
