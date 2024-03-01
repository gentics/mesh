package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.*;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedElement;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.frame.AbstractEdgeFrame;
import com.gentics.madl.graph.DelegatingFramedMadlGraph;
import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.dagger.OrientDBMeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.util.UUIDUtil;

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
	public Object id() {
		return super.getId();
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
	public DelegatingFramedMadlGraph<? extends Graph> getGraph() {
		return GraphDBTx.getGraphTx().getGraph();
	}

	@Override
	public Edge getElement() {
		// TODO FIXME We should store the element reference in a thread local map that is bound to the transaction. The references should be removed once the
		// transaction finishes
		Element edge = GraphDBTx.getGraphTx().getGraph().getBaseGraph().edges(id()).next();

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

	public static Edge findEdge(Object nodeId, String languageTag, String branchUuid, ContainerType type) {
		DelegatingFramedMadlGraph<? extends Graph> graph = GraphDBTx.getGraphTx().getGraph();
		OrientDBMeshComponent mesh = graph.getAttribute(GraphAttribute.MESH_COMPONENT);
		GraphDatabase db = mesh.database();
		Iterator<? extends Edge> iterator = graph.maybeGetIndexedFramedElements("e." + HAS_FIELD_CONTAINER.toLowerCase() + "_branch_type_lang",	db.index().createComposedIndexKey(nodeId, branchUuid, type.getCode(), languageTag), Edge.class)
				.orElseGet(() -> graph.getRawTraversal().E()
						.has(BRANCH_UUID_KEY, branchUuid)
						.has(EDGE_TYPE_KEY, type.getCode())
						.outV()
						.hasId(nodeId)
						.inE(HAS_FIELD_CONTAINER)
						.has(BRANCH_UUID_KEY, branchUuid)
						.has(EDGE_TYPE_KEY, type.getCode()));
		if (iterator.hasNext()) {
			return iterator.next();
		} else {
			return null;
		}
	}
}
