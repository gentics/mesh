package com.gentics.madl.frame;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedElement;

import com.gentics.madl.graph.DelegatingFramedMadlGraph;
import com.gentics.madl.traversal.EdgeTraversal;
import com.gentics.madl.traversal.EdgeTraversalImpl;
import com.gentics.madl.traversal.VertexTraversal;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.syncleus.ferma.AbstractElementFrame;
import com.syncleus.ferma.FramedGraph;

/**
 * Mesh specific vertex implementation. It must override all calls which would access the element via the field reference.
 * We don't use the ferma field. Instead the element will always be located using the stored elementId.
 * The Edge or Vertex instance of the {@link AbstractElementFrame#element} is thread bound and can't be shared across threads.
 * We thus need to avoid access to it.
 */
public abstract class AbstractEdgeFrame extends com.syncleus.ferma.AbstractEdgeFrame implements EdgeFrame {

	private Object id;

	@Override
	protected void init(FramedGraph graph, Element element) {
		super.init(graph, null);
		element.property(TYPE_RESOLUTION_KEY, getClass().getSimpleName());
		this.id = element.id();
	}

	@Override
	public <N> N getId() {
		return (N) id;
	}

	@Override
	public Edge getElement() {
		DelegatingFramedMadlGraph<? extends Graph> fg = getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}

		Edge edge = fg.getBaseGraph().edges(id).next();

		// Unwrap wrapped vertex
		if (edge instanceof WrappedElement) {
			edge = ((WrappedElement<Edge>) edge).getBaseElement();
		}
		return (Edge) edge;
	}

	@Override
	public DelegatingFramedMadlGraph<? extends Graph> getGraph() {
		return (DelegatingFramedMadlGraph<? extends Graph>) super.getGraph();
	}

	@Override
	public Element element() {
		return getElement();
	}

	/**
	 * @deprecated Replaced by {@link #label()}
	 */
	@Override
	@Deprecated
	public String getLabel() {
		return super.getLabel();
	}

	@Override
	public <T extends VertexFrame> TraversalResult<? extends T> inV(Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(traversal().outV().frameExplicit(clazz));
		return result;
	}

	@Override
	public <T extends VertexFrame> TraversalResult<? extends T> outV(Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(traversal().outV().frameExplicit(clazz));
		return result;
	}

	@Override
	public String label() {
		return getLabel();
	}

	@Override
	public EdgeTraversal<?, ?> traversal() {
		DelegatingFramedMadlGraph<? extends Graph> fg = getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}
		return new EdgeTraversalImpl<>(new DefaultGraphTraversal<>(fg.getRawTraversal()));
	}

	@Override
	public VertexTraversal<?, ?> inV() {
		return traversal().inV();
	}

	@Override
	public VertexTraversal<?, ?> outV() {
		return traversal().outV();
	}
}
