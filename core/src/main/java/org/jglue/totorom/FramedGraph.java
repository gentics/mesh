package org.jglue.totorom;

import java.io.Closeable;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

/**
 * The primary class for framing your blueprints graphs.
 * 
 * @author Bryn Cooke (http://jglue.org)
 */

public class FramedGraph {
	private Graph delegate;

	private TypeResolver resolver;
	private FrameFactory builder;

	/**
	 * Construct a framed graph.
	 * 
	 * @param delegate
	 *            The graph to wrap.
	 * @param builder
	 *            The builder that will construct frames.
	 * @param resolver
	 *            The type resolver that will decide the final frame type.
	 */
	public FramedGraph(Graph delegate, FrameFactory builder, TypeResolver resolver) {
		this.delegate = delegate;
		this.resolver = resolver;
		this.builder = builder;
	}

	/**
	 * Construct an untyped framed graph using no special frame construction.
	 * 
	 * @param delegate
	 *            The graph to wrap.
	 */
	public FramedGraph(Graph delegate) {
		this(delegate, FrameFactory.Default, TypeResolver.Untyped);
	}

	/**
	 * @return A transaction object that is {@link Closeable}.
	 */
	public Transaction tx() {
		if (delegate instanceof TransactionalGraph) {
			return new Transaction((TransactionalGraph) delegate);
		} else {
			return new Transaction((TransactionalGraph) null);
		}
	}

	/**
	 * Close the delegate graph.
	 */
	public void close() {
		delegate.shutdown();
	}

	public <T extends FramedElement> T frameElement(Element e, Class<T> kind) {
		if (e == null) {
			return null;
		}

		Class<T> frameType = (kind == TVertex.class || kind == TEdge.class) ? kind : resolver.resolve(e, kind);

		T framedElement = builder.create(e, frameType);
		framedElement.init(this, e);
		return framedElement;
	}

	<T extends FramedElement> T frameNewElement(Element e, Class<T> kind) {
		T t = frameElement(e, kind);
		resolver.init(e, kind);
		return t;
	}

	public <T extends FramedElement> Iterator<T> frame(Iterator<? extends Element> pipeline, final Class<T> kind) {
		return Iterators.transform(pipeline, new Function<Element, T>() {

			@Override
			public T apply(Element element) {
				return frameElement(element, kind);
			}

		});
	}

	/**
	 * Add a vertex to the graph
	 * 
	 * @param kind
	 *            The kind of the frame.
	 * @return The framed vertex.
	 */
	public <T extends FramedVertex> T addVertex(Class<T> kind) {
		T framedVertex = frameNewElement(delegate.addVertex(null), kind);
		framedVertex.init();
		return framedVertex;
	}

	/**
	 * Add a vertex to the graph using a frame type of {@link TVertex}.
	 * 
	 * @return The framed vertex.
	 */
	public TVertex addVertex() {

		return addVertex(TVertex.class);
	}

	/**
	 * Query over all vertices in the graph.
	 * 
	 * @return The query.
	 */
	public VertexTraversal<?, ?, ?> V() {
		return new GlobalVertexTraversal(this, delegate);
	}

	/**
	 * Query over all edges in the graph.
	 * 
	 * @return The query.
	 */
	public EdgeTraversal<?, ?, ?> E() {
		return new TraversalImpl(this, delegate).E();
	}

	/**
	 * Query over a list of vertices in the graph.
	 * 
	 * @param ids
	 *            The ids of the vertices.
	 * @return The query.
	 */
	public VertexTraversal<?, ?, ?> v(final Collection<?> ids) {
		return new TraversalImpl(this, Iterators.transform(ids.iterator(), new Function<Object, Vertex>() {

			@Override
			public Vertex apply(Object id) {
				return delegate.getVertex(id);
			}

		})).castToVertices();
	}

	/**
	 * Query over a list of vertices in the graph.
	 * 
	 * @param ids
	 *            The ids of the vertices.
	 * @return The query.
	 */
	public VertexTraversal<?, ?, ?> v(final Object... ids) {
		return new TraversalImpl(this, Iterators.transform(Iterators.forArray(ids), new Function<Object, Vertex>() {

			@Override
			public Vertex apply(Object id) {
				return delegate.getVertex(id);
			}

		})).castToVertices();
	}

	/**
	 * Query over a list of edges in the graph.
	 * 
	 * @param ids
	 *            The ids of the edges.
	 * @return The query.
	 */
	public EdgeTraversal<?, ?, ?> e(final Object... ids) {
		return new TraversalImpl(this, Iterators.transform(Iterators.forArray(ids), new Function<Object, Edge>() {

			@Override
			public Edge apply(Object id) {
				return delegate.getEdge(id);
			}

		})).castToEdges();
	}

	/**
	 * Query over a list of edges in the graph.
	 * 
	 * @param ids
	 *            The ids of the edges.
	 * @return The query.
	 */
	public EdgeTraversal<?, ?, ?> e(final Collection<?> ids) {
		return new TraversalImpl(this, Iterators.transform(ids.iterator(), new Function<Object, Edge>() {

			@Override
			public Edge apply(Object id) {
				return delegate.getEdge(id);
			}

		})).castToEdges();
	}
	
	public Graph getGraph() {
		return delegate;
	}

}
