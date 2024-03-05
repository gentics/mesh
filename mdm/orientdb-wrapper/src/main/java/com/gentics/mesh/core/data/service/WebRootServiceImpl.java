package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.GraphFieldContainerEdge.BRANCH_UUID_KEY;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.EDGE_TYPE_KEY;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.WEBROOT_URLFIELD_PROPERTY_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.madl.graph.DelegatingFramedMadlGraph;
import com.gentics.mesh.cache.WebrootPathCache;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * @see WebRootService
 */
@Singleton
public class WebRootServiceImpl extends AbstractWebRootService {

	@Inject
	public WebRootServiceImpl(Database database, WebrootPathCache pathStore) {
		super(database, pathStore);
	}

	@Override
	public NodeGraphFieldContainer findByUrlFieldPath(String branchUuid, String path, ContainerType type) {
		DelegatingFramedMadlGraph<? extends Graph> graph = GraphDBTx.getGraphTx().getGraph();
		GraphTraversal<Edge, Vertex> iter = graph.getRawTraversal().E()
			.hasLabel(GraphRelationships.HAS_FIELD_CONTAINER)
			.has(EDGE_TYPE_KEY, type.getCode())
			.has(WEBROOT_URLFIELD_PROPERTY_KEY, path)
			.has(BRANCH_UUID_KEY, branchUuid)
			.inV();
		if (iter.hasNext()) {
			return graph.frameElementExplicit(iter.next(), NodeGraphFieldContainerImpl.class);
		} else {
			return null;
		}
	}

}
