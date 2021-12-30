package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static com.gentics.mesh.core.rest.admin.consistency.RepairAction.DELETE;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.Iterator;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.util.VersionNumber;

import com.syncleus.ferma.FramedGraph;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Consistency check for node contents.
 */
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
			Iterable<GraphFieldContainerEdgeImpl> initialEdges = container.inE(HAS_FIELD_CONTAINER)
					.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.INITIAL.getCode()).frameExplicit(GraphFieldContainerEdgeImpl.class);
			if (!initialEdges.iterator().hasNext()) {

				boolean repaired = false;
				if (attemptRepair) {
					// printVersions(container);
					try {
						repaired = repair(container);
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

	/**
	 * This fix will create the missing {@link Node} for {@link NodeGraphFieldContainer}'s which were affected by the deletion bug which was fixed in version
	 * 0.18.3. Due to this bug the {@link Node} was deleted leaving the {@link NodeGraphFieldContainer} dangling in the graph.
	 */
	public boolean repair(HibNodeFieldContainer hibContainer) {
		ContentDao contentDao = Tx.get().contentDao();
		NodeGraphFieldContainer container = toGraph(hibContainer);
		MeshComponent mesh = container.getGraphAttribute(GraphAttribute.MESH_COMPONENT);
		BootstrapInitializer boot = mesh.boot();
		// Pick the first project we find to fetch the initial branchUuid
		HibProject project = boot.projectDao().findAll().iterator().next();
		String branchUuid = project.getInitialBranch().getUuid();

		HibSchemaVersion version = container.getSchemaContainerVersion();
		if (version == null) {
			log.error("Container {" + container.getUuid() + "} has no schema version linked to it.");
			return false;
		}
		HibSchema schemaContainer = version.getSchemaContainer();
		// 1. Find the initial version to check whether the whole version history is still intact
		HibNodeFieldContainer initial = findInitial(container);

		if (initial == null) {
			// The container has no previous version or is not the initial version so we can just delete it.
			container.remove();
			return true;
		}
		HibNodeFieldContainer latest = findLatest(container);
		HibNodeFieldContainer published = null;
		HibNodeFieldContainer draft = null;
		if (latest.getVersion().getFullVersion().endsWith(".0")) {
			published = latest;
		} else {
			draft = latest;
		}

		if (published == null) {
			published = findPublished(latest);
		}
		if (draft == null) {
			draft = findDraft(latest);
		}

		log.info("Initial:" + initial.getUuid() + " version: " + initial.getVersion());
		if (draft != null) {
			log.info("Draft:" + draft.getUuid() + " version: " + draft.getVersion());
		} else {
			throw new RuntimeException("The draft version could not be found");
		}
		if (published != null) {
			log.info("Publish:" + published.getUuid() + " version: " + published.getVersion());
		} else {
			log.info("Published not found");
		}

		log.info("Schema container " + schemaContainer.getName());

		FramedGraph graph = container.getGraph();
		Node node = graph.addFramedVertex(NodeImpl.class);
		node.setProject(project);
		node.setCreated(project.getCreator());

		if (published != null) {
			GraphFieldContainerEdge edge = node.addFramedEdge(HAS_FIELD_CONTAINER, toGraph(published), GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(published.getLanguageTag());
			edge.setBranchUuid(branchUuid);
			edge.setType(PUBLISHED);
		}

		GraphFieldContainerEdge edge = node.addFramedEdge(HAS_FIELD_CONTAINER, toGraph(draft), GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(draft.getLanguageTag());
		edge.setBranchUuid(branchUuid);
		edge.setType(DRAFT);

		GraphFieldContainerEdge initialEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
		initialEdge.setLanguageTag(initial.getLanguageTag());
		initialEdge.setBranchUuid(branchUuid);
		initialEdge.setType(INITIAL);

		BulkActionContext bac = mesh.bulkProvider().get();
		node.delete(bac);
		return true;
	}

	/**
	 * Iterate over all versions and try to find the latest published version.
	 *
	 * @param latest
	 * @return
	 */
	private HibNodeFieldContainer findPublished(HibNodeFieldContainer latest) {
		HibNodeFieldContainer previous = latest.getPreviousVersion();
		while (previous != null) {
			if (previous.getVersion().getFullVersion().equalsIgnoreCase(".0")) {
				return previous;
			}
			previous = previous.getPreviousVersion();
		}
		return null;

	}

	private HibNodeFieldContainer findLatest(HibNodeFieldContainer container) {
		Iterator<HibNodeFieldContainer> it = container.getNextVersions().iterator();
		if (it.hasNext()) {
			HibNodeFieldContainer next = it.next();
			if (it.hasNext()) {
				throw new RuntimeException("The version history has branches. The fix is currently unable to deal with version branches.");
			}
			return findLatest(next);
		} else {
			return container;
		}
	}

	private HibNodeFieldContainer findInitial(HibNodeFieldContainer container) {
		if (container.getVersion().getFullVersion().equalsIgnoreCase("0.1")) {
			return container;
		}
		HibNodeFieldContainer initial = null;
		HibNodeFieldContainer previous = container.getPreviousVersion();
		while (previous != null) {
			initial = previous;
			previous = previous.getPreviousVersion();
		}

		return initial;
	}

	private HibNodeFieldContainer findDraft(HibNodeFieldContainer latest) {
		HibNodeFieldContainer previous = latest.getPreviousVersion();
		while (previous != null) {
			if (!previous.getVersion().getFullVersion().equalsIgnoreCase(".0")) {
				return previous;
			}
			previous = previous.getPreviousVersion();
		}
		return null;
	}

}
