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

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.gremlin.Tokens;

/**
 * Specialized global vertex traversal that bypasses gremlin pipeline for simple key value lookups. As soon as a more complex traversal is detected then it
 * delegates to a full gremlin pipeline.
 */
@Deprecated
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
	public VertexTraversal<?, ?, M> hasNot(final String key) {
		return this.delegate().hasNot(key);
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
	public VertexTraversal<?, ?, M> has(Class<?> clazz) {
		return this.delegate().has(clazz);
	}

	@Override
	public VertexTraversal<?, ?, M> hasNot(final String key, final Object value) {
		return this.delegate().hasNot(key, value);
	}

	@Override
	public VertexTraversal<?, ?, M> out(final String... labels) {
		return this.simpleDelegate().out(labels);
	}

	@Override
	public VertexTraversal<?, ?, M> in(final String... labels) {
		return this.simpleDelegate().in(labels);
	}

	@Override
	public EdgeTraversal<?, ?, M> outE(final String... labels) {
		return this.simpleDelegate().outE(labels);
	}

	@Override
	public EdgeTraversal<?, ?, M> inE(final String... labels) {
		return this.simpleDelegate().inE(labels);
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
	public <N> Iterable<N> frameExplicit(final Class<N> kind) {
		return this.simpleDelegate().frameExplicit(kind);
	}

	@Override
	public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkOut(final String label, final VertexFrame other) {
		return this.simpleDelegate().linkOut(label, other);
	}

	@Override
	public VertexTraversal<List<EdgeFrame>, EdgeFrame, M> linkIn(final String label, final VertexFrame other) {
		return this.simpleDelegate().linkIn(label, other);
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
	public VertexTraversal<C, S, ? extends VertexTraversal<C, S, M>> mark() {
		return this.simpleDelegate().mark();
	}

	@Override
	public void removeAll() {
		this.simpleDelegate().removeAll();
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
	public void iterate() {
		Iterators.size(simpleIterator());
	}

	@Override
	public M back() {
		return this.simpleDelegate().back();
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
	public List<? extends VertexFrame> toList() {
		return this.simpleDelegate().toList();
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
