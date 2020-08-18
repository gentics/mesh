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
package com.syncleus.ferma;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.syncleus.ferma.framefactories.DefaultFrameFactory;
import com.syncleus.ferma.framefactories.FrameFactory;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.GlobalVertexTraversal;
import com.syncleus.ferma.traversals.SimpleTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.syncleus.ferma.typeresolvers.UntypedTypeResolver;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Features;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedGraph;

public class DelegatingFramedGraph<G extends Graph> extends WrappedGraph<G> implements WrapperFramedGraph<G> {

	private final TypeResolver defaultResolver;
	private final TypeResolver untypedResolver;
	private final FrameFactory builder;
	private final Map<String, Object> attributes = new HashMap<>();

	/**
	 * Construct a framed graph.
	 *
	 * @param delegate
	 *            The graph to wrap.
	 * @param builder
	 *            The builder that will construct frames.
	 * @param defaultResolver
	 *            The type defaultResolver that will decide the final frame type.
	 */
	public DelegatingFramedGraph(final G delegate, final FrameFactory builder, final TypeResolver defaultResolver) {
		super(delegate);

		if (builder == null)
			throw new IllegalArgumentException("builder can not be null");
		else if (defaultResolver == null)
			throw new IllegalArgumentException("defaultResolver can not be null");

		this.defaultResolver = defaultResolver;
		this.untypedResolver = new UntypedTypeResolver();
		this.builder = builder;
	}

	/**
	 * Construct a framed graph without annotation support.
	 *
	 * @param delegate
	 *            The graph to wrap.
	 * @param defaultResolver
	 *            The type defaultResolver that will decide the final frame type.
	 */
	public DelegatingFramedGraph(final G delegate, final TypeResolver defaultResolver) {
		this(delegate, new DefaultFrameFactory(), defaultResolver);
	}

	/**
	 * Close the delegate graph.
	 */
	@Override
	public void close() {
		this.getBaseGraph().shutdown();
	}

	@Override
	public <T> T frameElement(final Element e, final Class<T> kind) {
		if (e == null) {
			return null;
		}

		final Class<? extends T> frameType = (kind == TVertex.class || kind == TEdge.class) ? kind : defaultResolver.resolve(e, kind);

		final T frame = builder.create(e, frameType);
		((AbstractElementFrame) frame).init(this, e, e.getId());
		return frame;
	}

	@Override
	public <T> T frameNewElement(final Element e, final ClassInitializer<T> initializer) {
		final T frame = frameElement(e, initializer.getInitializationType());
		defaultResolver.init(e, initializer.getInitializationType());
		((AbstractElementFrame) frame).init();
		initializer.initalize(frame);
		return frame;
	}

	@Override
	public <T> T frameNewElement(final Element e, final Class<T> kind) {
		return this.frameNewElement(e, new DefaultClassInitializer<>(kind));
	}

	@Override
	public <T> Iterator<? extends T> frame(final Iterator<? extends Element> pipeline, final Class<T> kind) {
		return Iterators.transform(pipeline, new Function<Element, T>() {

			@Override
			public T apply(final Element input) {
				return frameElement(input, kind);
			}

		});
	}

	@Override
	public <T> T frameElementExplicit(final Element e, final Class<T> kind) {
		if (e == null) {
			return null;
		}

		final Class<? extends T> frameType = this.untypedResolver.resolve(e, kind);

		final T frame = builder.create(e, frameType);
		((AbstractElementFrame) frame).init(this, e, e.getId());
		return frame;
	}

	@Override
	public <T> T frameElementExplicitById(final Object id, final Class<T> kind) {
		if (id == null) {
			return null;
		}

		final Class<? extends T> frameType = this.untypedResolver.resolve(null, kind);

		final T frame = builder.create(null, frameType);
		((AbstractElementFrame) frame).init(this, null, id);
		return frame;
	}

	@Override
	public <T> T frameNewElementExplicit(final Element e, final ClassInitializer<T> initializer) {
		final T frame = frameElement(e, initializer.getInitializationType());
		this.untypedResolver.init(e, initializer.getInitializationType());
		((AbstractElementFrame) frame).init();
		initializer.initalize(frame);
		return frame;
	}

	@Override
	public <T> T frameNewElementExplicit(final Element e, final Class<T> kind) {
		return this.frameNewElementExplicit(e, new DefaultClassInitializer<>(kind));
	}

	@Override
	public <T> Iterator<T> frameExplicit(final Iterator<? extends Element> pipeline, final Class<T> kind) {
		return Iterators.transform(pipeline, new Function<Element, T>() {

			@Override
			public T apply(final Element input) {
				return frameElementExplicit(input, kind);
			}

		});
	}

	@Override
	public <T> T addFramedVertex(Object id, final ClassInitializer<T> initializer) {
		final T framedVertex = frameNewElement(this.getBaseGraph().addVertex(id), initializer);
		return framedVertex;
	}

	@Override
	public <T> T addFramedVertex(final Class<T> kind) {
		return this.addFramedVertex(null, new DefaultClassInitializer<>(kind));
	}

	@Override
	public <T> T addFramedVertexExplicit(final ClassInitializer<T> initializer) {
		final T framedVertex = frameNewElementExplicit(this.getBaseGraph().addVertex(null), initializer);
		return framedVertex;
	}

	@Override
	public <T> T addFramedVertexExplicit(final Class<T> kind) {
		return this.addFramedVertexExplicit(new DefaultClassInitializer<>(kind));
	}

	@Override
	public TVertex addFramedVertex() {
		return addFramedVertex(null, TVertex.DEFAULT_INITIALIZER);
	}

	@Override
	public <T> T addFramedEdge(final Object id, final VertexFrame source, final VertexFrame destination, final String label,
		final ClassInitializer<T> initializer) {
		final T framedEdge = frameNewElement(this.getBaseGraph().addEdge(id, source.getElement(), destination.getElement(), label), initializer);
		return framedEdge;
	}

	@Override
	public <T> T addFramedEdge(final VertexFrame source, final VertexFrame destination, final String label, final Class<T> kind) {
		return this.addFramedEdge(null, source, destination, label, new DefaultClassInitializer<>(kind));
	}

	@Override
	public TEdge addFramedEdge(final VertexFrame source, final VertexFrame destination, final String label) {
		return addFramedEdge(null, source, destination, label, TEdge.DEFAULT_INITIALIZER);
	}

	@Override
	public VertexTraversal<?, ?, ?> v() {
		return new GlobalVertexTraversal(this, this.getBaseGraph());
	}

	@Override
	public EdgeTraversal<?, ?, ?> e() {
		return new SimpleTraversal(this, this.getBaseGraph()).e();
	}

	@Override
	public <F> F getFramedVertexExplicit(Class<F> classOfF, Object id) {
		return frameElementExplicitById(id, classOfF);
	}

	@Override
	public <F> Iterable<? extends F> getFramedVertices(final String key, final Object value, final Class<F> kind) {
		return new FramingVertexIterable<>(this, this.getVertices(key, value), kind);
	}

	@Override
	public <F> Iterable<? extends F> getFramedVerticesExplicit(final Class<F> kind) {
		return new FramingVertexIterable<>(this, this.getVertices(), kind, true);
	}

	@Override
	public <F> Iterable<? extends F> getFramedVerticesExplicit(final String key, final Object value, final Class<F> kind) {
		return new FramingVertexIterable<>(this, this.getVertices(key, value), kind, true);
	}

	@Override
	public <F> Iterable<? extends F> getFramedEdges(final Class<F> kind) {
		return new FramingEdgeIterable<>(this, this.getEdges(), kind);
	}

	@Override
	public <F> Iterable<? extends F> getFramedEdges(final String key, final Object value, final Class<F> kind) {
		return new FramingEdgeIterable<>(this, this.getEdges(key, value), kind);
	}

	@Override
	public <F> Iterable<? extends F> getFramedEdgesExplicit(final String key, final Object value, final Class<F> kind) {
		return new FramingEdgeIterable<>(this, this.getEdges(key, value), kind, true);
	}

	@Override
	public EdgeTraversal<?, ?, ?> e(final Object... ids) {
		return new SimpleTraversal(this, Iterators.transform(Iterators.forArray(ids), new Function<Object, Edge>() {

			@Override
			public Edge apply(final Object input) {
				return getBaseGraph().getEdge(input);
			}

		})).castToEdges();
	}

	@Override
	public TypeResolver getTypeResolver() {
		return this.defaultResolver;
	}

	@Override
	public Features getFeatures() {
		return this.getBaseGraph().getFeatures();
	}

	@Override
	public Vertex addVertex(final Object id) {
		final VertexFrame framedVertex = frameNewElement(this.getBaseGraph().addVertex(null), TVertex.DEFAULT_INITIALIZER);
		return framedVertex.getElement();
	}

	@Override
	public Vertex addVertexExplicit(final Object id) {
		return this.getBaseGraph().addVertex(null);
	}

	@Override
	public Edge addEdge(final Object id, final Vertex outVertex, final Vertex inVertex, final String label) {
		final EdgeFrame framedEdge = frameNewElement(this.getBaseGraph().addEdge(id, outVertex, inVertex, label), TEdge.DEFAULT_INITIALIZER);
		return framedEdge.getElement();
	}

	@Override
	public Edge addEdgeExplicit(final Object id, final Vertex outVertex, final Vertex inVertex, final String label) {
		return this.getBaseGraph().addEdge(id, outVertex, inVertex, label);
	}

	@Override
	public <T> T getAttribute(String key) {
		return (T) attributes.get(key);
	}

	@Override
	public void setAttribute(String key, Object value) {
		attributes.put(key, value);
	}
}
