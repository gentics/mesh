package com.gentics.madl.frame;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedElement;

import com.gentics.madl.graph.DelegatingFramedMadlGraph;
import com.gentics.madl.traversal.EdgeTraversal;
import com.gentics.madl.traversal.EdgeTraversalImpl;
import com.gentics.madl.traversal.VertexTraversal;
import com.gentics.madl.traversal.VertexTraversalImpl;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.util.StreamUtil;
import com.syncleus.ferma.AbstractElementFrame;
import com.syncleus.ferma.FramedGraph;

/**
 * Mesh specific vertex implementation. It must override all calls which would access the element via the field reference.
 * We don't use the ferma field. Instead the element will always be located using the stored elementId.
 * The Edge or Vertex instance of the {@link AbstractElementFrame#element} is thread bound and can't be shared across theads.
 * We thus need to avoid access to it.
 */
public abstract class AbstractVertexFrame extends com.syncleus.ferma.AbstractVertexFrame implements VertexFrame {

	private Object id;

	@Override
	protected void init(FramedGraph graph, Element element) {
		super.init(graph, null);
		this.id = element.id();
	}

	@Override
	public <N> N getId() {
		return (N) id;
	}

	@Override
	public Vertex getElement() {
		DelegatingFramedMadlGraph<? extends Graph> fg = getGraph();
		if (fg == null) {
			throw new RuntimeException("Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}

		Vertex vertex = fg.getBaseGraph().vertices(id).next();

		// Unwrap wrapped vertex
		if (vertex instanceof WrappedElement) {
			vertex = ((WrappedElement<Vertex>) vertex).getBaseElement();
		}
		return (Vertex) vertex;
	}

	@Override
	public DelegatingFramedMadlGraph<? extends Graph> getGraph() {
		return (DelegatingFramedMadlGraph<? extends Graph>) super.getGraph();
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
	public EdgeTraversal<?, ?> inE(String... labels) {
		FramedGraph fg = getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}
		DefaultGraphTraversal<?, ?> traversal = new DefaultGraphTraversal<>(fg.getRawTraversal());
		return new EdgeTraversalImpl<>(traversal.inE(labels));
	}

	@Override
	public EdgeTraversal<?, ?> outE(String... labels) {
		FramedGraph fg = getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}
		DefaultGraphTraversal<?, ?> traversal = new DefaultGraphTraversal<>(fg.getRawTraversal());
		return new EdgeTraversalImpl<>(traversal.outE(labels));
	}

	@Override
	public VertexTraversal<?, ?> in(String... labels) {
		FramedGraph fg = getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}
		DefaultGraphTraversal<?, ?> traversal = new DefaultGraphTraversal<>(fg.getRawTraversal());
		return new VertexTraversalImpl<>(traversal.in(labels));
	}

	@Override
	public VertexTraversal<?, ?> out(String... labels) {
		FramedGraph fg = getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}
		DefaultGraphTraversal<?, ?> traversal = new DefaultGraphTraversal<>(fg.getRawTraversal());
		return new VertexTraversalImpl<>(traversal.out(labels));
	}

	@Override
	public <T extends ElementFrame> Result<? extends T> out(String label, Class<T> clazz) {
		return new TraversalResult<>(StreamUtil.toStream(getElement().edges(Direction.OUT, label)).map(Edge::outVertex).filter(vertex -> vertex.getClass().equals(clazz)).map(clazz::cast));
	}

	@Override
	public <T extends EdgeFrame> Result<? extends T> outE(String label, Class<T> clazz) {
		return new TraversalResult<>(StreamUtil.toStream(getElement().edges(Direction.OUT, label)).filter(edge -> edge.getClass().equals(clazz)).map(clazz::cast));
	}

	@Override
	public <T extends ElementFrame> Result<? extends T> in(String label, Class<T> clazz) {
		return new TraversalResult<>(StreamUtil.toStream(getElement().edges(Direction.IN, label)).map(Edge::inVertex).filter(vertex -> vertex.getClass().equals(clazz)).map(clazz::cast));
	}

	@Override
	public <T extends EdgeFrame> Result<? extends T> inE(String label, Class<T> clazz) {
		return new TraversalResult<>(StreamUtil.toStream(getElement().edges(Direction.IN, label)).filter(edge -> edge.getClass().equals(clazz)).map(clazz::cast));
	}

	@Override
	public Element element() {
		return getElement();
	}
}
