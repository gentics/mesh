package com.gentics.madl.graph;

import java.util.Iterator;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.syncleus.ferma.WrappedFramedGraph;

public interface DelegatingFramedMadlGraph<G extends Graph> extends WrappedFramedGraph<G> {

	default <T> T getFramedVertexOrNull(Class<T> kind, Object id) {
		return this.traverse(input -> input.V(id)).nextOrDefault(kind, null);
	}

	default <T> T getFramedVertexExplicitOrNull(Class<T> kind, Object id) {
		return this.traverse(input -> input.V(id)).nextOrDefaultExplicit(kind, null);
	}

	default <T> Optional<Iterator<? extends T>> maybeGetIndexedFramedElements(final String index, final Object id,
			final Class<T> kind) {
		return Optional.empty();
	}

	/**
	 * Return the vertex referenced by the provided object identifier. If no vertex
	 * is referenced by that identifier, then return null.
	 *
	 * @param id the identifier of the vertex to retrieved from the graph
	 * @return the vertex referenced by the provided identifier or null when no such
	 *         vertex exists
	 */
	default Vertex getVertex(Object id) {
		Iterator<Vertex> iter = getBaseGraph().vertices(id);
		return iter.hasNext() ? iter.next() : null;
	}

	/**
	 * Remove the provided vertex from the graph. Upon removing the vertex, all the
	 * edges by which the vertex is connected must be removed as well.
	 *
	 * @param vertex the vertex to remove from the graph
	 */
	default void removeVertex(Vertex vertex) {
		vertex.remove();
	}

	/**
	 * Return an iterable to all the vertices in the graph. If this is not possible
	 * for the implementation, then an UnsupportedOperationException can be thrown.
	 *
	 * @return an iterable reference to all vertices in the graph
	 */
	default Iterable<Vertex> getVertices() {
		return () -> getBaseGraph().vertices();
	}

	/**
	 * Return an iterable to all the vertices in the graph that have a particular
	 * key/value property. If this is not possible for the implementation, then an
	 * UnsupportedOperationException can be thrown. The graph implementation should
	 * use indexing structures to make this efficient else a full vertex-filter scan
	 * is required.
	 *
	 * @param key   the key of vertex
	 * @param value the value of the vertex
	 * @return an iterable of vertices with provided key and value
	 */
	default Iterable<Vertex> getVertices(String key, Object value) {
		return traverse(input -> input.V().has(key, value));
	}

	/**
	 * Return the edge referenced by the provided object identifier. If no edge is
	 * referenced by that identifier, then return null.
	 *
	 * @param id the identifier of the edge to retrieved from the graph
	 * @return the edge referenced by the provided identifier or null when no such
	 *         edge exists
	 */
	default Edge getEdge(Object id) {
		Iterator<Edge> iter = getBaseGraph().edges(id);
		return iter.hasNext() ? iter.next() : null;
	}

	/**
	 * Remove the provided edge from the graph.
	 *
	 * @param edge the edge to remove from the graph
	 */
	default void removeEdge(Edge edge) {
		edge.remove();
	}

	/**
	 * Return an iterable to all the edges in the graph. If this is not possible for
	 * the implementation, then an UnsupportedOperationException can be thrown.
	 *
	 * @return an iterable reference to all edges in the graph
	 */
	default Iterable<Edge> getEdges() {
		return () -> getBaseGraph().edges();
	}

	/**
	 * Return an iterable to all the edges in the graph that have a particular
	 * key/value property. If this is not possible for the implementation, then an
	 * UnsupportedOperationException can be thrown. The graph implementation should
	 * use indexing structures to make this efficient else a full edge-filter scan
	 * is required.
	 *
	 * @param key   the key of the edge
	 * @param value the value of the edge
	 * @return an iterable of edges with provided key and value
	 */
	default Iterable<Edge> getEdges(String key, Object value) {
		return traverse(input -> input.E().has(key, value));
	}

	/**
	 * Get an arbitrary object attached to this graph.
	 * 
	 * @param <T>
	 * @param key
	 * @return
	 */
	<T> T getAttribute(String key);

	/**
	 * Attach an object to this graph.
	 * 
	 * @param key
	 * @param value
	 */
	void setAttribute(String key, Object value);
}
