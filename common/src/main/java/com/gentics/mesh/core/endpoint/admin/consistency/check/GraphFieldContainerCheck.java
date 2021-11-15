package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static com.gentics.mesh.core.rest.admin.consistency.RepairAction.DELETE;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.util.VersionNumber;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Consistency check for node contents.
 */
public abstract class GraphFieldContainerCheck<T extends HibNodeFieldContainer> extends AbstractContainerConsistencyCheck<T> {

	private static final Logger log = LoggerFactory.getLogger(GraphFieldContainerCheck.class);

	protected abstract boolean isContainerInitialForNode(T container);
	
	@Override
	public String getName() {
		return "node-contents";
	}

	@Override
	protected void checkContainer(Database db, T container, ConsistencyCheckResult result, boolean attemptRepair) {
		PersistingContentDao contentDao = CommonTx.get().contentDao();
		String uuid = container.getUuid();
		if (container.getSchemaContainerVersion() == null) {
			result.addInconsistency("The GraphFieldContainer has no assigned SchemaContainerVersion", uuid, HIGH);
		}
		VersionNumber version = container.getVersion();
		if (version == null) {
			result.addInconsistency("The GraphFieldContainer has no version number", uuid, HIGH);
		}
		if (container.getBucketId() == null) {
			result.addInconsistency("The GraphFieldContainer bucket id is not set", uuid, MEDIUM);
		}

		// GFC must either have a previous GFC, or must be the initial GFC for a Node
		HibNodeFieldContainer previous = container.getPreviousVersion();
		if (previous == null) {			
			if (!isContainerInitialForNode(container)) {
				boolean repaired = false;
				if (attemptRepair) {
					// printVersions(container);
					try {
						repaired = contentDao.repair(container);
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
					HibNodeFieldContainer next = contentDao.getNextVersions(container).iterator().next();
					if (next != null) {
						String tag = next.getLanguageTag();
						if (tag != null) {
							container.setLanguageTag(tag);
							info.setRepairAction(RepairAction.RECOVER).setRepaired(true);
						}
					}
				} else if (container.hasPreviousVersion()) {
					HibNodeFieldContainer prev = container.getPreviousVersion();
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
