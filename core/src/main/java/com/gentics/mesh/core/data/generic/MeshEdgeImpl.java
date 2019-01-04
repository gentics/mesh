package com.gentics.mesh.core.data.generic;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedEdge;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedElement;

import com.gentics.madl.annotation.GraphElement;
import com.gentics.madl.tx.Tx;
import com.gentics.madl.wrapper.element.AbstractWrappedEdge;
import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.Database;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;

/**
 * @see MeshEdge
 */
@GraphElement
public class MeshEdgeImpl extends AbstractWrappedEdge implements MeshEdge {

	private Object id;

	@Override
	protected void init() {
		super.init();
		property("uuid", UUIDUtil.randomUUID());
	}

	@Override
	protected void init(Database graph, Element element) {
		super.init(graph, element);
		this.id = element.id();
	}

	public String getFermaType() {
		return property(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
	}

	public String getUuid() {
		return value("uuid");
	}

	public void setUuid(String uuid) {
		property("uuid", uuid);
	}

	@Override
	public Database getGraph() {
		return Tx.getActive().getGraph();
	}

	@Override
	public Edge getElement() {
		// TODO FIXME We should store the element reference in a thread local map that is bound to the transaction. The references should be removed once the
		// transaction finishes
		Element edge = ((WrappedEdge) Tx.get().getEdge(id)).getBaseElement();

		// Element edge = threadLocalElement.get();

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
	public String getElementVersion() {
		Edge edge = getElement();
		return MeshInternal.get().database().getElementVersion(edge);
	}

}
