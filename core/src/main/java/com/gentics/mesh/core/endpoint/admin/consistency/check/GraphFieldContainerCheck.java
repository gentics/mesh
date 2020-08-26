package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static com.gentics.mesh.core.rest.admin.consistency.RepairAction.DELETE;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.endpoint.admin.consistency.repair.NodeDeletionGraphFieldContainerFix;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.VersionNumber;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GraphFieldContainerCheck extends AbstractConsistencyCheck {

	private static final Logger log = LoggerFactory.getLogger(GraphFieldContainerCheck.class);

	@Override
	public String getName() {
		return "node-contents";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, NodeGraphFieldContainerImpl.class, (element, result) -> {
			checkGraphFieldContainer(db, element, result, attemptRepair);
		}, attemptRepair, tx);
	}

	private void checkGraphFieldContainer(Database db, NodeGraphFieldContainer container, ConsistencyCheckResult result, boolean attemptRepair) {
		ContentDaoWrapper contentDao = Tx.get().data().contentDao();
		String uuid = container.getUuid();
		if (container.getSchemaContainerVersion() == null) {
			result.addInconsistency("The GraphFieldContainer has no assigned SchemaContainerVersion", uuid, HIGH);
		}
		VersionNumber version = container.getVersion();
		if (version == null) {
			result.addInconsistency("The GraphFieldContainer has no version number", uuid, HIGH);
		}

		// GFC must either have a previous GFC, or must be the initial GFC for a Node
		NodeGraphFieldContainer previous = container.getPreviousVersion();
		if (previous == null) {
			Iterable<GraphFieldContainerEdgeImpl> initialEdges = container.inE(HAS_FIELD_CONTAINER)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.INITIAL.getCode()).frameExplicit(GraphFieldContainerEdgeImpl.class);
			if (!initialEdges.iterator().hasNext()) {

				boolean repaired = false;
				if (attemptRepair) {
					// printVersions(container);
					try {
						repaired = new NodeDeletionGraphFieldContainerFix().repair(container);
					} catch (Exception e) {
						log.error("Error while repairing inconsistency", e);
						throw e;
					}
				}

				result.addInconsistency(
					String.format("GraphFieldContainer {" + version + "} does not have previous GraphFieldContainer and is not INITIAL for a Node"),
					uuid,
					MEDIUM,
					repaired,
					DELETE);
				return;

			}
		} else {
			VersionNumber previousVersion = previous.getVersion();
			if (previousVersion != null && version != null) {
				boolean notSameDraft = !version.equals(previousVersion.nextDraft());
				boolean notLargerVersion = version.compareTo(previousVersion.nextPublished()) > 1;
				if (notSameDraft && notLargerVersion) {
					String nodeInfo = "unknown";
					try {
						HibNode node = contentDao.getNode(container);
						nodeInfo = node.getUuid();
					} catch (Exception e) {
						log.debug("Could not load node uuid", e);
					}
					result.addInconsistency(
						String.format(
							"GraphFieldContainer of Node {" + nodeInfo
								+ "} has version %s which does not come after its previous GraphFieldContainer's version %s",
							version,
							previousVersion),
						uuid, MEDIUM);
				}
			}
		}

		// GFC must either have a next GFC, or must be the draft GFC for a Node
		if (!contentDao.hasNextVersion(container) && !container.isDraft()) {
			String nodeInfo = "unknown";
			try {
				HibNode node = contentDao.getNode(container);
				nodeInfo = node.getUuid();
			} catch (Exception e) {
				log.debug("Could not load node uuid", e);
			}
			result.addInconsistency(
				String.format("GraphFieldContainer {" + version + "} of Node {" + nodeInfo
					+ "} does not have next GraphFieldContainer and is not DRAFT for a Node"),
				uuid,
				MEDIUM);
		}

		// GFC must have a language
		if (container.getLanguageTag() == null) {
			InconsistencyInfo info = new InconsistencyInfo().setDescription("GraphFieldContainer {" + version + "} has no language set")
				.setElementUuid(uuid).setSeverity(MEDIUM);
			if (attemptRepair) {
				if (contentDao.hasNextVersion(container)) {
					NodeGraphFieldContainer next = contentDao.getNextVersions(container).iterator().next();
					if (next != null) {
						String tag = next.getLanguageTag();
						if (tag != null) {
							container.setLanguageTag(tag);
							info.setRepairAction(RepairAction.RECOVER).setRepaired(true);
						}
					}
				} else if (container.hasPreviousVersion()) {
					NodeGraphFieldContainer prev = container.getPreviousVersion();
					if (prev != null) {
						String tag = prev.getLanguageTag();
						if (tag != null) {
							container.setLanguageTag(tag);
							info.setRepairAction(RepairAction.RECOVER).setRepaired(true);
						}
					}
				}
			}
		}
	}

}
