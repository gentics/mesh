package com.gentics.mesh.graphdb.orientdb.graph;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedEdge;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;

/**
 * The intercepting edge frame is used in order to be able to switch the currently active graph database context within a single edge instance. This way a
 * framed edge can be used across multiple transactions without the need to reload the instance from the graph. This is mainly achieved by overriding the
 * {@link #getGraph()} method and using the {@link Database#getThreadLocalGraph()} method instead.
 */
public class AbstractInterceptingEdgeFrame extends AbstractEdgeFrame {

	private Object id;
	public ThreadLocal<Element> threadLocalElement = ThreadLocal.withInitial(() -> ((WrappedEdge) getGraph().getEdge(id)).getBaseElement());

	@Override
	protected void init(FramedGraph graph, Element element) {
		super.init(graph, element);
		this.id = element.getId();
	}

	public String getFermaType() {
		return getProperty(TYPE_RESOLUTION_KEY);
	}

	public String getUuid() {
		return getProperty("uuid");
	}

	public void setUuid(String uuid) {
		setProperty("uuid", uuid);
	}

	@Override
	public FramedGraph getGraph() {
		return Tx.getActive().getGraph();
	}

	@Override
	public Edge getElement() {
		Element edge = threadLocalElement.get();

		// Unwrap wrapped edge
		if (edge instanceof WrappedElement) {
			edge = (Edge) ((WrappedElement) edge).getBaseElement();
		}
		return (Edge) edge;
	}

}
