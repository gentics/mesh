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
 * When: November, 30th 2014
 */
package com.syncleus.ferma.traversals;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.DefaultClassInitializer;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.Path;
import com.syncleus.ferma.VertexFrame;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.gremlin.Tokens;
import com.tinkerpop.pipes.transform.TransformPipe;
import com.tinkerpop.pipes.util.structures.Pair;
import com.tinkerpop.pipes.util.structures.Row;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.pipes.util.structures.Tree;

import java.util.*;
import java.util.function.Consumer;

import org.apache.commons.lang.NotImplementedException;

/**
 * Specialized global vertex traversal that bypasses gremlin pipeline for simple key value lookups. As soon as a more complex traversal is detected then it
 * delegates to a full gremlin pipeline.
 */
public class GlobalVertexTraversal<C, S, M> implements VertexTraversal<C, S, M> {

	private final FramedGraph graph;

	private final Graph delegate;
	private String key;
	private Object value;

	private VertexTraversal<C, S, M> traversal;
	private Iterator iterator;

	public GlobalVertexTraversal(final FramedGraph graph, final Graph delegate) {
		this.graph = graph;
		this.delegate = delegate;

	}

	/**
	 * We've dropped out of what can be optimized. Time to create a proper traversal.
	 *
	 * @return
	 */
	private VertexTraversal<C, S, M> delegate() {
		if (traversal == null)
			if (key != null)
				traversal = (VertexTraversal<C, S, M>) new SimpleTraversal<VertexFrame, C, S, M>(graph, delegate).v().has(key, value);
			else
				traversal = (VertexTraversal<C, S, M>) new SimpleTraversal<VertexFrame, C, S, M>(graph, delegate).v();
		return traversal;
	}

	/**
	 * The traversal is still has a simple query, but is passed on to other gremlin pipeline steps. It is safe to use the simple iterator.
	 *
	 * @return
	 */
	private VertexTraversal<C, S, M> simpleDelegate() {
		if (traversal == null) {
			if (iterator != null)
				throw new IllegalStateException("Traversal cannot be modified after iteration has started");

			traversal = new SimpleTraversal<VertexFrame, C, S, M>(graph, simpleIterator()).castToVertices();
		}
		return traversal;
	}

	/**
	 * Used for simple iteration, uses the key index if available, but will defer to graph query if not.
	 *
	 * @return
	 */
	private Iterator<VertexFrame> simpleIterator() {
		if (iterator == null) {
			final Iterator delegateIterator;
			if (key != null)
				if (delegate instanceof TinkerGraph)
					// Tinker graph will do it's own check to see if it supports
					// the key
					delegateIterator = delegate.getVertices(key, value).iterator();
				else if (delegate instanceof KeyIndexableGraph)
					if (((KeyIndexableGraph) delegate).getIndexedKeys(Vertex.class).contains(key))
						// This graph supports lookups for this key
						delegateIterator = delegate.getVertices(key, value).iterator();
					else
						// This graph does not support lookup of this key, but
						// it may still be supported via the query interface.
						delegateIterator = delegate.query().has(key, value).vertices().iterator();
				else
					// This graph does not support lookup of this key, but it
					// may still be supported via the query interface.
					delegateIterator = delegate.query().has(key, value).vertices().iterator();
			else
				// There is no key so it is a full traversal.
				delegateIterator = delegate.getVertices().iterator();
			iterator = new Iterator() {
				private Element current;

				@Override
				public boolean hasNext() {
					return delegateIterator.hasNext();
				}

				@Override
				public Object next() {
					current = (Element) delegateIterator.next();
					return current;
				}

				@Override
				public void remove() {
					if (current != null) {
						current.remove();
						current = null;
					} else
						throw new IllegalStateException();
				}
			};
		}
		return iterator;
	}

	@Override
	public Iterator<VertexFrame> iterator() {
		return Iterators.transform(simpleIterator(), new Function() {

			@Override
			public Object apply(final Object e) {
				return graph.frameElement((Element) e, VertexFrame.class);
			}
		});
	}

	@Override
	public VertexTraversal<?, ?, M> has(final String key) {
		return this.delegate().has(key);
	}

	@Override
	public VertexTraversal<?, ?, M> has(final String key, final Object value) {
		if (this.key == null) {
			this.key = key;
			this.value = value;
			return this;
		} else
			return delegate().has(key, value);
	}

	@Override
	public VertexTraversal<?, ?, M> has(final String key, final Tokens.T compareToken, final Object value) {
		return this.delegate().has(key, compareToken, value);
	}

	@Override
	public VertexTraversal<?, ?, M> has(final String key, final Predicate predicate, final Object value) {
		return this.delegate().has(key, predicate, value);
	}

	@Override
	public VertexTraversal<?, ?, M> has(Class<?> clazz) {
		return this.delegate().has(clazz);
	}

	@Override
	public VertexTraversal<?, ?, M> hasNot(final String key) {
		return this.delegate().hasNot(key);
	}

	@Override
	public VertexTraversal<?, ?, M> hasNot(final String key, final Object value) {
		return this.delegate().hasNot(key, value);
	}

	@Override
	public VertexTraversal<?, ?, M> hasNot(Class<?> clazz) {
		return this.delegate().hasNot(clazz);
	}

	@Override
	public <Z> VertexTraversal<?, ?, M> interval(final String key, final Comparable<Z> startValue, final Comparable<Z> endValue) {
		return this.delegate().interval(key, startValue, endValue);
	}

	@Override
	public VertexTraversal<?, ?, M> out(final int branchFactor, final String... labels) {
		return this.simpleDelegate().out(branchFactor, labels);
	}

	@Override
	public VertexTraversal<?, ?, M> out(final String... labels) {
		return this.simpleDelegate().out(labels);
	}

	@Override
	public VertexTraversal<?, ?, M> in(final int branchFactor, final String... labels) {
		return this.simpleDelegate().in(branchFactor, labels);
	}

	@Override
	public VertexTraversal<?, ?, M> in(final String... labels) {
		return this.simpleDelegate().in(labels);
	}

	@Override
	public VertexTraversal<?, ?, M> both(final int branchFactor, final String... labels) {
		return this.simpleDelegate().both(branchFactor, labels);
	}

	@Override
	public VertexTraversal<?, ?, M> both(final String... labels) {
		return this.simpleDelegate().both(labels);
	}

	@Override
	public EdgeTraversal<?, ?, M> outE(final int branchFactor, final String... labels) {
		return this.simpleDelegate().outE(branchFactor, labels);
	}

	@Override
	public EdgeTraversal<?, ?, M> outE(final String... labels) {
		return this.simpleDelegate().outE(labels);
	}

	@Override
	public EdgeTraversal<?, ?, M> inE(final int branchFactor, final String... labels) {
		return this.simpleDelegate().inE(branchFactor, labels);
	}

	@Override
	public EdgeTraversal<?, ?, M> inE(final String... labels) {
		return this.simpleDelegate().inE(labels);
	}

	@Override
	public EdgeTraversal<?, ?, M> bothE(final int branchFactor, final String... labels) {
		return this.simpleDelegate().bothE(branchFactor, labels);
	}

	@Override
	public EdgeTraversal<?, ?, M> bothE(final String... labels) {
		return this.simpleDelegate().bothE(labels);
	}

	@Override
	public <N> N next(final Class<N> kind) {
		return graph.frameElement((Element) simpleIterator().next(), kind);
	}

	@Override
	public <N> N nextExplicit(final Class<N> kind) {
		return graph.frameElementExplicit((Element) simpleIterator().next(), kind);
	}

	@Override
	public <N> N nextOrDefault(final Class<N> kind, final N defaultValue) {
		if (simpleIterator().hasNext())
			return next(kind);
		else
			return defaultValue;
	}

	@Override
	public <N> N nextOrDefaultExplicit(final Class<N> kind, final N defaultValue) {
		if (simpleIterator().hasNext())
			return nextExplicit(kind);
		else
			return defaultValue;
	}

	@Override
	public VertexFrame nextOrAdd() {
		return this.simpleDelegate().nextOrAdd();
	}

	@Override
	public <N> N nextOrAddExplicit(final ClassInitializer<N> initializer) {
		return this.simpleDelegate().nextOrAddExplicit(initializer);
	}

	@Override
	public <N> N nextOrAddExplicit(final Class<N> kind) {
		return this.nextOrAddExplicit(new DefaultClassInitializer<>(kind));
	}

	@Override
	public <N> N nextOrAdd(final ClassInitializer<N> initializer) {
		return this.delegate().nextOrAdd(initializer);
	}

	@Override
	public <N> N nextOrAdd(final Class<N> kind) {
		return this.nextOrAdd(new DefaultClassInitializer<>(kind));
	}

	@Override
	public <N> List<? extends N> next(final int amount, final Class<N> kind) {
		return this.delegate().next(amount, kind);
	}

	@Override
	public <N> List<? extends N> nextExplicit(final int amount, final Class<N> kind) {
		return this.delegate().nextExplicit(amount, kind);
	}

	@Override
	public <N> Iterable<N> frame(final Class<N> kind) {
		final Iterator transform = Iterators.transform(simpleIterator(), new Function() {

			@Override
			public Object apply(final Object e) {
				return graph.frameElement((Element) e, kind);
			}
		});
		return new Iterable<N>() {

			@Override
			public Iterator<N> iterator() {
				return transform;
			}
		};
	}

	@Override
	public <N> Iterable<? extends N> frameExplicit(final Class<N> kind) {
		return this.simpleDelegate().frameExplicit(kind);
	}

	@Override
	public <N> List<? extends N> toList(final Class<N> kind) {
		return this.simpleDelegate().toList(kind);
	}

	@Override
	public <N> List<? extends N> toListExplicit(final Class<N> kind) {
		return this.simpleDelegate().toListExplicit(kind);
	}

	@Override
	public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkOut(final String label, final String namedStep) {
		return this.simpleDelegate().linkOut(label, namedStep);
	}

	@Override
	public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkIn(final String label, final String namedStep) {
		return this.simpleDelegate().linkIn(label, namedStep);
	}

	@Override
	public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkBoth(final String label, final String namedStep) {
		return this.simpleDelegate().linkBoth(label, namedStep);
	}

	@Override
	public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkOut(final String label, final Vertex other) {
		return this.simpleDelegate().linkOut(label, other);
	}

	@Override
	public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkOut(final String label, final VertexFrame other) {
		return this.simpleDelegate().linkOut(label, other);
	}

	@Override
	public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkIn(final String label, final Vertex other) {
		return this.simpleDelegate().linkIn(label, other);
	}

	@Override
	public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkBoth(final String label, final Vertex other) {
		return this.simpleDelegate().linkBoth(label, other);
	}

	@Override
	public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkIn(final String label, final VertexFrame other) {
		return this.simpleDelegate().linkIn(label, other);
	}

	@Override
	public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkBoth(final String label, final VertexFrame other) {
		return this.simpleDelegate().linkBoth(label, other);
	}

	@Override
	public VertexTraversal<?, ?, M> dedup() {
		return this.simpleDelegate().dedup();
	}

	@Override
	public VertexTraversal<?, ?, M> dedup(final TraversalFunction<VertexFrame, ?> dedupFunction) {
		return this.simpleDelegate().dedup(dedupFunction);
	}

	@Override
	public VertexTraversal<?, ?, M> except(final Iterable<?> collection) {
		return this.simpleDelegate().except(collection);
	}

	@Override
	public VertexTraversal<?, ?, M> except(final VertexFrame... vertices) {
		return this.simpleDelegate().except(vertices);
	}

	@Override
	public VertexTraversal<?, ?, M> except(final String... namedSteps) {
		return this.simpleDelegate().except(namedSteps);
	}

	@Override
	public VertexTraversal<?, ?, M> filter(final TraversalFunction<VertexFrame, Boolean> filterFunction) {
		return this.simpleDelegate().filter(filterFunction);
	}

	@Override
	public VertexTraversal<?, ?, M> retain(final VertexFrame... vertices) {
		return this.simpleDelegate().retain(vertices);
	}

	@Override
	public VertexTraversal<?, ?, M> retain(final Iterable<?> collection) {
		return this.simpleDelegate().retain(collection);
	}

	@Override
	public VertexTraversal<?, ?, M> retain(final String... namedSteps) {
		return this.simpleDelegate().retain(namedSteps);
	}

	@Override
	public VertexTraversal<Collection<? extends VertexFrame>, Collection<? extends VertexFrame>, M> aggregate() {
		return this.simpleDelegate().aggregate();
	}

	@Override
	public VertexTraversal<Collection<? extends VertexFrame>, Collection<? extends VertexFrame>, M> aggregate(
		final Collection<? super VertexFrame> aggregate) {
		return this.simpleDelegate().aggregate(aggregate);
	}

	@Override
	public <N> VertexTraversal<Collection<? extends N>, Collection<? extends N>, M> aggregate(final Collection<? super N> aggregate,
		final TraversalFunction<VertexFrame, ? extends N> aggregateFunction) {
		return this.simpleDelegate().aggregate(aggregate, aggregateFunction);
	}

	@Override
	public <N> VertexTraversal<Collection<? extends N>, Collection<? extends N>, M> aggregate(
		final TraversalFunction<VertexFrame, ? extends N> aggregateFunction) {
		return this.simpleDelegate().aggregate(aggregateFunction);
	}

	@Override
	public VertexTraversal<?, ?, M> sideEffect(final SideEffectFunction<VertexFrame> sideEffectFunction) {
		return this.simpleDelegate().sideEffect(sideEffectFunction);
	}

	@Override
	public VertexTraversal<?, ?, M> identity() {
		return this.simpleDelegate().identity();
	}

	@Override
	public VertexTraversal<?, ?, M> memoize(final String namedStep) {
		return this.simpleDelegate().memoize(namedStep);
	}

	@Override
	public VertexTraversal<?, ?, M> memoize(final String namedStep, final Map<?, ?> map) {
		return this.simpleDelegate().memoize(namedStep, map);
	}

	@Override
	public VertexTraversal<?, ?, M> order() {
		return this.simpleDelegate().order();
	}

	@Override
	public VertexTraversal<?, ?, M> order(final Comparator<? super VertexFrame> compareFunction) {
		return this.simpleDelegate().order(compareFunction);
	}

	@Override
	public VertexTraversal<?, ?, M> order(final TransformPipe.Order order) {
		return this.simpleDelegate().order(order);
	}

	@Override
	public VertexTraversal<?, ?, M> order(final Tokens.T order) {
		return this.simpleDelegate().order(order);
	}

	@Override
	public <N> Collection<? extends N> fill(final Collection<? super N> collection, final Class<N> kind) {
		return this.simpleDelegate().fill(collection, kind);
	}

	@Override
	public <N> Collection<? extends N> fillExplicit(final Collection<? super N> collection, final Class<N> kind) {
		return this.simpleDelegate().fillExplicit(collection, kind);
	}

	@Override
	public VertexTraversal<?, ?, M> and(final TraversalFunction<VertexFrame, Traversal<?, ?, ?, ?>>... traversals) {
		return this.simpleDelegate().and(traversals);
	}

	@Override
	public VertexTraversal<?, ?, M> or(final TraversalFunction<VertexFrame, Traversal<?, ?, ?, ?>>... traversals) {
		return this.simpleDelegate().or(traversals);
	}

	@Override
	public VertexTraversal<C, S, ? extends VertexTraversal<C, S, M>> mark() {
		return this.simpleDelegate().mark();
	}

	@Override
	public void removeAll() {
		this.simpleDelegate().removeAll();
	}

	@Override
	public <N> SplitTraversal<? extends Traversal<N, ?, ?, M>> copySplit(
		final TraversalFunction<VertexFrame, ? extends Traversal<N, ?, ?, ?>>... traversals) {
		return this.simpleDelegate().copySplit(traversals);
	}

	@Override
	public VertexTraversal<?, ?, M> loop(final TraversalFunction<VertexFrame, ? extends VertexTraversal<?, ?, ?>> traversal) {
		return this.simpleDelegate().loop(traversal);
	}

	@Override
	public VertexTraversal<?, ?, M> loop(final TraversalFunction<VertexFrame, ? extends VertexTraversal<?, ?, ?>> traversal, final int depth) {
		return this.simpleDelegate().loop(traversal, depth);
	}

	@Override
	public VertexTraversal<?, ?, M> v() {
		return this;
	}

	@Override
	public EdgeTraversal<?, ?, M> e() {
		return this.simpleDelegate().e();
	}

	@Override
	public VertexTraversal<?, ?, M> v(final String key, final Object value) {
		return this.simpleDelegate().v(key, value);
	}

	@Override
	public void iterate() {
		Iterators.size(simpleIterator());
	}

	@Override
	public M back() {
		return this.simpleDelegate().back();
	}

	@Override
	public C cap() {
		return this.simpleDelegate().cap();
	}

	@Override
	public <N> Traversal<? extends N, ?, ?, M> transform(final TraversalFunction<VertexFrame, N> function) {
		return this.simpleDelegate().transform(function);
	}

	@Override
	public <N> Traversal<N, ?, ?, M> start(final N object) {
		return this.simpleDelegate().start(object);
	}

	@Override
	public VertexTraversal<?, ?, M> start(final VertexFrame object) {
		return this.simpleDelegate().start(object);
	}

	@Override
	public EdgeTraversal<?, ?, M> start(final EdgeFrame object) {
		return this.simpleDelegate().start(object);
	}

	@Override
	public <N> Traversal<N, ?, ?, M> property(final String key) {
		return this.simpleDelegate().property(key);
	}

	@Override
	public <N> Traversal<? extends N, ?, ?, M> property(final String key, final Class<N> type) {
		return this.simpleDelegate().property(key, type);
	}

	@Override
	public long count() {
		return Iterators.size(simpleIterator());
	}

	@Override
	public VertexFrame next() {
		return this.next(VertexFrame.class);
	}

	@Override
	public VertexFrame nextOrDefault(final VertexFrame defaultValue) {
		return nextOrDefault(VertexFrame.class, defaultValue);
	}

	@Override
	public List<? extends VertexFrame> next(final int number) {
		return this.simpleDelegate().next(number);
	}

	@Override
	public List<? extends VertexFrame> toList() {
		return this.simpleDelegate().toList();
	}

	@Override
	public Traversal<VertexFrame, C, S, M> enablePath() {
		return this.simpleDelegate().enablePath();
	}

	@Override
	public Collection<? extends VertexFrame> fill(final Collection<? super VertexFrame> collection) {
		return this.simpleDelegate().fill(collection);
	}

	@Override
	public <N> Traversal<N, ?, ?, M> id() {
		return this.simpleDelegate().id();
	}

	@Override
	public <N> Traversal<? extends N, ?, ?, M> id(final Class<N> c) {
		return this.simpleDelegate().id(c);
	}

	@Override
	public boolean hasNext() {
		return this.simpleIterator().hasNext();
	}

	@Override
	public void remove() {
		this.simpleIterator().remove();
	}

	@Override
	public void forEachRemaining(final Consumer<? super VertexFrame> consumer) {
		this.simpleDelegate().forEachRemaining(consumer);
	}

	@Override
	public void forEach(final Consumer<? super VertexFrame> consumer) {
		this.simpleDelegate().forEach(consumer);
	}

	@Override
	public Spliterator<VertexFrame> spliterator() {
		return this.simpleDelegate().spliterator();
	}

}
