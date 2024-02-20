package com.gentics.madl.frame;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStep;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedEdge;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedElement;

import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.syncleus.ferma.FramedGraph;

public abstract class AbstractEdgeFrame extends com.syncleus.ferma.AbstractEdgeFrame implements EdgeFrame {

	@Override
	public Edge getElement() {
		FramedGraph fg = getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}

		Edge edgeForId = fg.getRawTraversal().E(id()).tryNext().orElseThrow(() -> new RuntimeException("No edge for Id {" + id() + "} of type {" + getClass().getName() + "} could be found within the graph"));
		Element edge = ((WrappedEdge<Edge>) edgeForId).getBaseEdge();

		// Unwrap wrapped vertex
		if (edge instanceof WrappedElement) {
			edge = ((WrappedElement<Edge>) edge).getBaseElement();
		}
		return (Edge) edge;

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
		DefaultGraphTraversal<?, ?> traversal = traversal();
		return new TraversalResult<>(traversal.addStep(new GraphStep(traversal, clazz, false)).inV());
	}

	@Override
	public <T extends VertexFrame> TraversalResult<? extends T> outV(Class<T> clazz) {
		DefaultGraphTraversal<?, ?> traversal = traversal();
		return new TraversalResult<>(traversal.addStep(new GraphStep(traversal, clazz, false)).outV());
	}

	@Override
	public String label() {
		return getLabel();
	}

	@Override
	public DefaultGraphTraversal<?, ?> traversal() {
		FramedGraph fg = getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}
		return new DefaultGraphTraversal<>(fg.getRawTraversal());
	}
}
