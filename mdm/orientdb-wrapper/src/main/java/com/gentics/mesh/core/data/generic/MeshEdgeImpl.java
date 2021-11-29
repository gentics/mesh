package com.gentics.mesh.core.data.generic;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.core.db.AbstractEdgeFrame;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.dagger.OrientDBMeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedEdge;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;

import io.vertx.core.Vertx;

/**
 * @see MeshEdge
 */
@GraphElement
public class MeshEdgeImpl extends AbstractEdgeFrame implements MeshEdge {

	@Override
	protected void init() {
		super.init();
		setProperty("uuid", UUIDUtil.randomUUID());
	}

	@Override
	protected void init(FramedGraph graph, Element e, Object id) {
		super.init(graph, null, id);
	}

	public String getFermaType() {
		return property(TYPE_RESOLUTION_KEY);
	}

	public String getUuid() {
		return property("uuid");
	}

	/**
	 * Manually set the uuid.
	 * 
	 * @param uuid
	 */
	public void setUuid(String uuid) {
		property("uuid", uuid);
	}

	@Override
	public FramedGraph getGraph() {
		return GraphDBTx.getGraphTx().getGraph();
	}

	@Override
	public Edge getElement() {
		// TODO FIXME We should store the element reference in a thread local map that is bound to the transaction. The references should be removed once the
		// transaction finishes
		Element edge = ((WrappedEdge) GraphDBTx.getGraphTx().getGraph().getEdge(id())).getBaseElement();

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
		return db().getElementVersion(edge);
	}

	/**
	 * Return the dagger mesh context from the graph attributes. The component is accessed this way since it is not otherwise possible to inject dagger into
	 * domain classes.
	 * 
	 * @return
	 */
	public OrientDBMeshComponent mesh() {
		return getGraphAttribute(GraphAttribute.MESH_COMPONENT);
	}

	@Override
	public GraphDatabase db() {
		return mesh().database();
	}

	@Override
	public Vertx vertx() {
		return mesh().vertx();
	}

	@Override
	public MeshOptions options() {
		return mesh().options();
	}

}
