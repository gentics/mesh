package com.gentics.madl.frame;

import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStep;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedElement;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedVertex;

import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.syncleus.ferma.FramedGraph;

public abstract class AbstractVertexFrame extends com.syncleus.ferma.AbstractVertexFrame implements VertexFrame {

	@Override
	public Vertex getElement() {
		FramedGraph fg = getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}

		Vertex vertexForId = fg.getRawTraversal().V(id()).tryNext().orElseThrow(() -> new RuntimeException("No vertex for Id {" + id() + "} of type {" + getClass().getName() + "} could be found within the graph"));
		Element vertex = (Element) ((WrappedVertex<?>) vertexForId).getBaseVertex();

		// Unwrap wrapped vertex
		if (vertex instanceof WrappedElement) {
			vertex = (Vertex) ((WrappedElement<?>) vertex).getBaseElement();
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
		// unlinkOut(null, labels);
		getElement().edges(Direction.OUT, labels).forEachRemaining(Element::remove);
		// Create a new edge with the given label
		// linkOut(vertex, labels);
		for (String label : labels) {
			getElement().addEdge(label, vertex.getElement());
		}
	}

	@Override
	public <T extends ElementFrame> GraphTraversal<?, ?> traversalIn(Class<T> clazz, String... labels) {
		FramedGraph fg = getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}
		DefaultGraphTraversal<?, ?> traversal = new DefaultGraphTraversal<>(fg.getRawTraversal());
		return traversal.addStep(new GraphStep(traversal, clazz, false, labels)).in(labels);
	}

	@Override
	public <T extends ElementFrame> GraphTraversal<?, ?> traversalOut(Class<T> clazz, String... labels) {
		FramedGraph fg = getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}
		DefaultGraphTraversal<?, ?> traversal = new DefaultGraphTraversal<>(fg.getRawTraversal());
		return traversal.addStep(new GraphStep(traversal, clazz, false, labels)).out(labels);
	}
}
