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
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.pipes.FermaGremlinPipeline;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Predicate;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.pipes.transform.TransformPipe.Order;

/**
 * Vertex specific traversal. This class is abstract and as such is never instantiated directly.
 *
 * @param <C>
 *            The cap of the current pipe.
 * @param <S>
 *            The SideEffect of the current pipe.
 * @param <M>
 *            The current marked type for the current pipe.
 */
@Deprecated
abstract class AbstractVertexTraversal<C, S, M> extends AbstractTraversal<VertexFrame, C, S, M> implements VertexTraversal<C, S, M> {
	protected AbstractVertexTraversal(FramedGraph graph, FermaGremlinPipeline pipeline) {
		super(graph, pipeline);
	}

	@Override
	public VertexFrame next() {
		return graph().frameElement((Vertex) getPipeline().next(), VertexFrame.class);
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
	public VertexTraversal<?, ?, M> filter(final TraversalFunction<VertexFrame, Boolean> filterFunction) {
		return (VertexTraversal<?, ?, M>) super.filter(filterFunction);
	}

	@Override
	public VertexTraversal<?, ?, M> dedup(final TraversalFunction<VertexFrame, ?> dedupFunction) {
		return (VertexTraversal) super.dedup(dedupFunction);
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
	public VertexTraversal<?, ?, M> out(final String... labels) {
		getPipeline().out(labels);
		return this;
	}

	@Override
	public VertexTraversal<?, ?, M> in(final String... labels) {
		getPipeline().in(labels);
		return this;
	}

	@Override
	public EdgeTraversal<?, ?, M> outE(final String... labels) {
		getPipeline().outE(labels);
		return castToEdges();
	}

	@Override
	public EdgeTraversal<?, ?, M> inE(final String... labels) {
		getPipeline().inE(labels);
		return castToEdges();
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
	public <N> Iterable<N> frame(final Class<N> kind) {
		return Iterables.transform(getPipeline(), new Function() {

			@Override
			public Object apply(final Object input) {
				return graph().frameElement((Element) input, kind);
			}
		});
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
	public VertexTraversal<?, ?, M> retain(final VertexFrame... vertices) {
		return (VertexTraversal<?, ?, M>) super.retain(Arrays.asList(vertices));
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

}
