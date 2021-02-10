package com.gentics.mesh.core.db;

import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.syncleus.ferma.AbstractElementFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedEdge;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;

/**
 * OrientDB specific edge frame implementation. It must override all calls which would access the element via the field reference.
 * We don't use the ferma field in Gentics Mesh. Instead the element will always be located using the stored elementId.
 * The Edge or Vertex instance of the {@link AbstractElementFrame#element} is thread bound and can't be shared across theads.
 * We thus need to avoid access to it.
 */
public abstract class AbstractEdgeFrame extends com.syncleus.ferma.AbstractEdgeFrame implements EdgeFrame {

	@Override
	public Edge getElement() {
		FramedGraph fg = ((GraphDBTx) Tx.get()).getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}

		Edge edgeForId = fg.getEdge(id);
		if (edgeForId == null) {
			throw new RuntimeException("No edge for Id {" + id + "} of type {" + getClass().getName() + "} could be found within the graph");
		}
		Element edge = ((WrappedEdge) edgeForId).getBaseElement();

		// Unwrap wrapped vertex
		if (edge instanceof WrappedElement) {
			edge = (Edge) ((WrappedElement) edge).getBaseElement();
		}
		return (Edge) edge;

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
	public VertexTraversal<?, ?, ?> inV() {
		return super.inV();
	}

	@Override
	public <T extends VertexFrame> TraversalResult<? extends T> inV(Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(inV().frameExplicit(clazz));
		return result;
	}

	@Override
	public VertexTraversal<?, ?, ?> outV() {
		return super.outV();
	}

	@Override
	public <T extends VertexFrame> TraversalResult<? extends T> outV(Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(outV().frameExplicit(clazz));
		return result;
	}

	@Override
	public EdgeTraversal<?, ?, ?> traversal() {
		return super.traversal();
	}

	@Override
	public <T> T reframe(Class<T> kind) {
		return super.reframe(kind);
	}

	@Override
	public <T> T reframeExplicit(Class<T> kind) {
		return super.reframeExplicit(kind);
	}

	@Override
	public String label() {
		return getLabel();
	}

	@Override
	public void remove() {
		super.remove();
	}
}
