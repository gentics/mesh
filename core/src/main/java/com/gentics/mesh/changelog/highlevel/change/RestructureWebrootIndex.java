package com.gentics.mesh.changelog.highlevel.change;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Change which will get rid of the old {@link NodeGraphFieldContainer} webroot properties and instead add those props to the HAS_FIELD_CONTAINER edge.
 */
@Singleton
public class RestructureWebrootIndex extends AbstractHighLevelChange {

	private static final Logger log = LoggerFactory.getLogger(RestructureWebrootIndex.class);

	private final Database db;

	@Inject
	public RestructureWebrootIndex(Database db) {
		this.db = db;
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
		log.info("Applying change: " + getName());
		FramedTransactionalGraph graph = Tx.getActive().getGraph();
		Iterable<? extends GraphFieldContainerEdgeImpl> edges = graph.getFramedEdgesExplicit("@class", HAS_FIELD_CONTAINER,
			GraphFieldContainerEdgeImpl.class);
		long count = 0;
		for (GraphFieldContainerEdgeImpl edge : edges) {
			ContainerType type = edge.getType();
			if (DRAFT.equals(type) || PUBLISHED.equals(type)) {
				String branchUuid = edge.getBranchUuid();
				NodeGraphFieldContainer container = edge.getNodeContainer();
				// Skip graph inconsistencies
				if (container == null) {
					continue;
				}
				Node node = container.getParentNode();
				if (node != null) {
					node = node.getParentNode(branchUuid);
				}
				edge.setUrlFieldInfo(container.getUrlFieldValues());
				String segment = container.getSegmentFieldValue();
				if (segment != null && !segment.trim().isEmpty()) {
					String newInfo = GraphFieldContainerEdgeImpl.composeSegmentInfo(node, segment);
					edge.setSegmentInfo(newInfo);
				} else {
					edge.setSegmentInfo(null);
				}
				if (count % 100 == 0) {
					log.info("Updating edge {" + count + "}");
				}
				count++;
			}

			String segment = edge.getSegmentInfo();
			if (segment == null || segment.trim().isEmpty()) {
				edge.setSegmentInfo(null);
			}

		}
		log.info("Done updating all edges. Total: {" + count + "}");

		Iterable<? extends NodeGraphFieldContainerImpl> containers = graph.getFramedVertices("@class",
			NodeGraphFieldContainerImpl.class.getSimpleName(), NodeGraphFieldContainerImpl.class);
		for (NodeGraphFieldContainer container : containers) {
			container.getElement().removeProperty("publishedWebrootUrlInfo");
			container.getElement().removeProperty("webrootUrlInfo");
			container.getElement().removeProperty("publishedWebrootPathInfo");
			container.getElement().removeProperty("webrootPathInfo");
		}

	}

	@Override
	public void applyNoTx() {
		db.removeVertexIndex("webrootPathInfoIndex", NodeGraphFieldContainerImpl.class);
		db.removeVertexIndex("publishedWebrootPathInfoIndex", NodeGraphFieldContainerImpl.class);
		db.removeVertexIndex("webrootUrlInfoIndex", NodeGraphFieldContainerImpl.class);
		db.removeVertexIndex("publishedWebrootInfoIndex", NodeGraphFieldContainerImpl.class);
	}

}
