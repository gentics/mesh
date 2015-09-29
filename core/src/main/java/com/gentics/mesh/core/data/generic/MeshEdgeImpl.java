package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedEdge;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;

public class MeshEdgeImpl extends AbstractEdgeFrame implements MeshEdge {

	private Object id;
	public ThreadLocal<Element> threadLocalElement = ThreadLocal.withInitial(() -> ((WrappedEdge) getGraph().getEdge(id)).getBaseElement());

	@Override
	protected void init() {
		super.init();
		setProperty("uuid", UUIDUtil.randomUUID());
	}

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

	public MeshEdgeImpl getImpl() {
		return this;
	}

	@Override
	public void reload() {
		MeshSpringConfiguration.getInstance().database().reload(this);
	}

}
