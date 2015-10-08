package com.gentics.mesh.graphdb.orientdb.graph;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedEdge;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;

public class AbstractInterceptingEdgeFrame extends AbstractEdgeFrame {

	private Object id;
	public ThreadLocal<Element> threadLocalElement = ThreadLocal.withInitial(() -> ((WrappedEdge) getGraph().getEdge(id)).getBaseElement());

	@Override
	protected void init(FramedGraph graph, Element element) {
		super.init(graph, element);
		this.id = element.getId();
	}

	public String getFermaType() {
		return getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
	}

	public String getUuid() {
		return getProperty("uuid");
	}

	public void setUuid(String uuid) {
		setProperty("uuid", uuid);
	}

	@Override
	public FramedGraph getGraph() {
		return new DelegatingFramedGraph<>(Database.getThreadLocalGraph(), true, false);
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
