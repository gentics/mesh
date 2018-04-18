package com.gentics.mesh.core.endpoint.admin.consistency.asserter;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.VersionNumber;

import java.util.Iterator;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

public class GraphFieldContainerCheck implements ConsistencyCheck {
	@Override
	public void invoke(Database db, ConsistencyCheckResponse response) {
		Iterator<? extends NodeGraphFieldContainerImpl> it = db.getVerticesForType(NodeGraphFieldContainerImpl.class);
		while (it.hasNext()) {
			checkGraphFieldContainer(it.next(), response);
		}
	}

	private void checkGraphFieldContainer(NodeGraphFieldContainerImpl container, ConsistencyCheckResponse response) {
		String uuid = container.getUuid();
		if (container.getSchemaContainerVersion() == null) {
			response.addInconsistency("The GraphFieldContainer has no assigned SchemaContainerVersion",
				uuid, HIGH);
		}
		VersionNumber version = container.getVersion();
		if (version == null) {
			response.addInconsistency("The GraphFieldContainer has no version", uuid, HIGH);
		}
		// GFC must either have a previous GFC, or must be the initial GFC for a Node
		NodeGraphFieldContainer previous = container.getPreviousVersion();
		if (previous == null) {
			Iterable<GraphFieldContainerEdgeImpl> initialEdges = container.inE(HAS_FIELD_CONTAINER)
					.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.INITIAL.getCode()).frameExplicit(GraphFieldContainerEdgeImpl.class);
			if (!initialEdges.iterator().hasNext()) {
				response.addInconsistency(
						String.format("GraphFieldContainer does not have previous GraphFieldContainer and is not INITIAL for a Node"), uuid,
						MEDIUM);
			}
		} else {
			VersionNumber previousVersion = previous.getVersion();
			if (previousVersion != null && version != null) {
				if (!version.equals(previousVersion.nextDraft()) && !version.equals(previousVersion.nextPublished())) {
					response.addInconsistency(
							String.format("GraphFieldContainer has version %s which does not come after its previous GraphFieldContainer's version %s", version,
									previousVersion),
							uuid, MEDIUM);
				}
			}
		}

		// GFC must either have a next GFC, or must be the draft GFC for a Node
		if (!container.hasNextVersion() && !container.isDraft()) {
			response.addInconsistency(String.format("GraphFieldContainer does not have next GraphFieldContainer and is not DRAFT for a Node"), uuid, MEDIUM);
		}
	}
}
