package com.gentics.mesh.changelog.highlevel.change;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class RestructureWebrootIndex extends AbstractHighLevelChange {

	private static final Logger log = LoggerFactory.getLogger(RestructureWebrootIndex.class);

	@Inject
	public RestructureWebrootIndex() {
	}

	@Override
	public String getUuid() {
		return "7E94C51E763C46D394C51E763C86D3F5";
	}

	@Override
	public String getName() {
		return "Restructure Webroot Index";
	}

	@Override
	public String getDescription() {
		return "Restructures the webroot index by iterating over all publish and draft edges.";
	}

	@Override
	public void apply() {
		FramedTransactionalGraph graph = Tx.getActive().getGraph();
		String key = "@class";
		Object value = HAS_FIELD_CONTAINER;
		Iterable<? extends GraphFieldContainerEdgeImpl> edges = graph.getFramedEdgesExplicit(key, value, GraphFieldContainerEdgeImpl.class);
		for (GraphFieldContainerEdgeImpl edge : edges) {
			ContainerType type = edge.getType();
			if (DRAFT.equals(type) || PUBLISHED.equals(type)) {
				String branchUuid = edge.getBranchUuid();
				NodeGraphFieldContainer container = edge.getNodeContainer();
				Node node = container.getParentNode();
				if (node != null) {
					node = node.getParentNode(branchUuid);
				}
				edge.setUrlFieldInfo(container.getUrlFieldValues());
				String newInfo = GraphFieldContainerEdgeImpl.composeSegmentInfo(node,
					container.getSegmentFieldValue());
				edge.setSegmentInfo(newInfo);
			}
		}
	}

}
