package com.gentics.madl.frame;

import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.madl.tx.BaseTransaction;
import com.gentics.madl.tx.Tx;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

public abstract class AbstractVertexFrame extends com.syncleus.ferma.AbstractVertexFrame implements VertexFrame {

	/**
	 * @deprecated Replaced by {@link #id()}
	 */
	@Deprecated
	@Override
	public Object getId() {
		return super.getId();
	}

	@Override
	public Vertex getElement() {
		// TODO FIXME We should store the element reference in a thread local map that is bound to the transaction. The references should be removed once the
		FramedGraph fg = Tx.get().getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}

		Vertex vertexForId = fg.getVertex(id);
		if (vertexForId == null) {
			throw new RuntimeException("No vertex for Id {" + id + "} of type {" + getClass().getName() + "} could be found within the graph");
		}
		Element vertex = ((WrappedVertex) vertexForId).getBaseElement();

		// Unwrap wrapped vertex
		if (vertex instanceof WrappedElement) {
			vertex = (Vertex) ((WrappedElement) vertex).getBaseElement();
		}
		return (Vertex) vertex;
	}

	@Override
	public Object id() {
		return getId();
	}

	@Override
	public <T> T getProperty(String name) {
		return super.getProperty(name);
	}

	@Override
	public Set<String> getPropertyKeys() {
		return super.getPropertyKeys();
	}

	@Override
	public void remove() {
		super.remove();
	}

	@Override
	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		// Unlink all edges between both objects with the given label
		unlinkOut(vertex, labels);
		// Create a new edge with the given label
		linkOut(vertex, labels);
	}

	/**
	 * Add a single link <b>in-bound</b> link to the given vertex. Note that this method will remove all other links to other vertices for the given labels and
	 * only create a single edge between both vertices per label.
	 * 
	 * @param vertex
	 *            Target vertex
	 * @param labels
	 *            Labels to handle
	 */
	@Override
	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		// Unlink all edges with the given label
		unlinkIn(null, labels);
		// Create a new edge with the given label
		linkIn(vertex, labels);
	}

	public void unlinkIn(final VertexFrame vertex, final String... labels) {
		for (String label : labels) {
			Iterator<Edge> it = getEdges(Direction.IN, label);
			while (it.hasNext()) {
				Edge edge = it.next();
				if (vertex != null) {
					Vertex outV = edge.getVertex(Direction.OUT);
					if (outV.getId().equals(vertex.id())) {
						continue;
					}
				} else {
					edge.remove();
				}
			}
		}
	}

	public void unlinkOut(final VertexFrame vertex, final String... labels) {
		for (String label : labels) {
			Iterator<Edge> it = getEdges(Direction.OUT, label);
			while (it.hasNext()) {
				Edge edge = it.next();
				if (vertex != null) {
					Vertex outV = edge.getVertex(Direction.IN);
					if (outV.getId().equals(vertex.id())) {
						continue;
					}
				} else {
					edge.remove();
				}
			}
		}
	}

	public void setLinkOut(final VertexFrame vertex, final String... labels) {
		unlinkOut(null, labels);
		if (vertex != null)
			linkOut(vertex, labels);
	}

	/**
	 * Add a unique <b>in-bound</b> link to the given vertex for the given set of labels. Note that this method will effectively ensure that only one
	 * <b>in-bound</b> link exists between the two vertices for each label.
	 * 
	 * @param vertex
	 *            Target vertex
	 * @param labels
	 *            Labels to handle
	 */
	@Override
	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		// Unlink all edges between both objects with the given label
		unlinkIn(vertex, labels);
		// Create a new edge with the given label
		linkIn(vertex, labels);
	}

	/**
	 * Remove all out-bound edges with the given label from the current vertex and create a new new <b>out-bound</b> edge between the current and given vertex
	 * using the specified label. Note that only a single out-bound edge per label will be preserved.
	 * 
	 * @param vertex
	 *            Target vertex
	 * @param labels
	 *            Labels to handle
	 */
	@Override
	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		// Unlink all edges with the given label
		getElement().getEdges(Direction.OUT, labels).forEach(Element::remove);
		// Create a new edge with the given label
		for (String label : labels) {
			getElement().addEdge(label, vertex.getElement());
		}
	}

	@Override
	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(out(label).frameExplicit(clazz));
		return result;
	}

	@Override
	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(outE(label).frameExplicit(clazz));
		return result;
	}

	@Override
	public Stream<Edge> streamOutE(String label) {
		return toStream(getEdges(Direction.OUT, label));
	}
	
	@Override
	public Stream<Vertex> streamOut(String label) {
		return toStream(getVertices(Direction.OUT, label));
	}

	@Override
	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(in(label).frameExplicit(clazz));
		return result;
	}

	@Override
	public Stream<Edge> streamInE(String label) {
		return toStream(getEdges(Direction.IN, label));
	}
	
	@Override
	public Stream<Vertex> streamIn(String label) {
		return toStream(getVertices(Direction.IN, label));
	}

	@Override
	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(inE(label).frameExplicit(clazz));
		return result;
	}

	@Override
	public <T extends RawTraversalResult<?>> T traverse(final Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		BaseTransaction tx = Tx.get();
		if (tx == null) {
			throw new RuntimeException("No active transaction found.");
		}
		return tx.traversal(input -> traverser.apply(input.V(id())));
	}

	public void linkOut(final VertexFrame vertex, final String... labels) {
		for (final String label : labels) {
			getElement().addEdge(label, vertex.getElement());
		}
	}

	public void linkIn(final VertexFrame vertex, final String... labels) {
		for (final String label : labels) {
			vertex.getElement().addEdge(label, this.getElement());
		}
	}

	private Iterator<Edge> getEdges(Direction dir, String label) {
		return getElement().getEdges(dir, label).iterator();
	}

	private Iterator<Vertex> getVertices(Direction dir, String label) {
		return getElement().getVertices(dir, label).iterator();
	}

	private <T> Stream<T> toStream(Iterator<T> it) {
		Stream<T> stream = StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED),
			false);
		return stream;
	}
}
