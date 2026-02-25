package com.gentics.mesh.check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.VersionNumber;

import jakarta.persistence.EntityManager;

/**
 * Check consistency of node contents. Check whether a node has not more than one DRAFT or PUBLISHED content per branch and language
 */
public class NodeContentConsistencyCheck extends AbstractHibernateConsistencyCheck {
	/**
	 * Severity of found inconsistencies
	 */
	protected final static InconsistencySeverity SEVERITY = InconsistencySeverity.HIGH;

	/**
	 * Repair action for found inconsistencies
	 */
	protected final static RepairAction REPAIR_ACTION = RepairAction.DELETE;

	/**
	 * Description for found inconsistencies
	 */
	protected final static String DESCRIPTION = "Node %s has %d contents of type %s for language %s in branch %s";

	/**
	 * Filter for edges (filter for edges that point to a container with a version)
	 */
	protected final static Predicate<HibNodeFieldContainerEdgeImpl> EDGE_FILTER = e -> {
		return Optional.ofNullable(e).map(HibNodeFieldContainerEdgeImpl::getNodeContainer)
				.map(HibNodeFieldContainer::getVersion).isPresent();
	};

	/**
	 * Comparator for edges, which compares the versions of the containers
	 */
	protected final static Comparator<HibNodeFieldContainerEdgeImpl> EDGE_COMPARATOR = (e1, e2) -> {
		Optional<VersionNumber> version1 = Optional.ofNullable(e1).map(HibNodeFieldContainerEdgeImpl::getNodeContainer)
				.map(HibNodeFieldContainer::getVersion);
		Optional<VersionNumber> version2 = Optional.ofNullable(e2).map(HibNodeFieldContainerEdgeImpl::getNodeContainer)
				.map(HibNodeFieldContainer::getVersion);

		if (version1.isPresent() && version2.isPresent()) {
			return version1.get().compareTo(version2.get());
		}
		return 0;
	};

	/**
	 * Check whether the given content is a successor of the given initial content
	 * @param content content to check
	 * @param initialContent initial content
	 * @return true when the content is a successor of the given initial content, false if not
	 */
	protected static boolean isSuccessorOf(HibNodeFieldContainer content, HibNodeFieldContainer initialContent) {
		if (initialContent == null) {
			return false;
		}
		HibNodeFieldContainer tmp = content;

		while (tmp != null && !Objects.equals(tmp, initialContent)) {
			tmp = tmp.getPreviousVersion();
		}

		return Objects.equals(tmp, initialContent);
	}

	/**
	 * Check whether the given content is a successor of any content with given UUIDs
	 * @param content content to check
	 * @param initialContentsUuids uuids of (initial) contents
	 * @return true when the content is a successor of any of the given contents, false if not
	 */
	protected static boolean isSuccessorOfAny(HibNodeFieldContainer content, Set<String> initialContentsUuids) {
		HibNodeFieldContainer tmp = content;

		while (tmp != null) {
			if (initialContentsUuids.contains(tmp.getUuid())) {
				return true;
			}
			tmp = tmp.getPreviousVersion();
		}

		return false;
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		checkLinkConsistency(tx, result, attemptRepair);
		return result;
	}

	@Override
	public String getName() {
		return "nodecontent_versions";
	}

	/**
	 * Check consistency of node versions
	 * @param tx transaction
	 * @param result check result
	 * @param attemptRepair true to attempt repair
	 */
	protected void checkLinkConsistency(Tx tx, ConsistencyCheckResult result, boolean attemptRepair) {
		HibernateTx hibernateTx = (HibernateTx) tx;
		EntityManager em = hibernateTx.entityManager();
		NodeDao nodeDao = hibernateTx.nodeDao();
		BranchDao branchDao = hibernateTx.branchDao();
		PersistingContentDao contentDao = hibernateTx.contentDao();

		String nfcTableName = MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "nodefieldcontainer";

		// get duplicate edges
		String sql = "SELECT node_dbuuid, languagetag, type, branch_dbuuid, COUNT(*) c FROM %s GROUP BY languagetag, type, node_dbuuid, branch_dbuuid HAVING COUNT(*) > 1"
				.formatted(nfcTableName);

		@SuppressWarnings("unchecked")
		List<Object[]> results = em.createNativeQuery(sql, Object[].class).getResultList();

		for (Object[] row : results) {
			String nodeUuid = UUIDUtil.toShortUuid(row[0].toString());
			String languageTag = row[1].toString();
			String type = row[2].toString();
			String branchUuid = UUIDUtil.toShortUuid(row[3].toString());
			int count = (int) row[4];
			HibNode node = nodeDao.findByUuidGlobal(nodeUuid);
			HibProject project = Optional.ofNullable(node).map(HibNode::getProject).orElse(null);
			HibBranch branch = Optional.ofNullable(project).map(p -> branchDao.findByUuid(p, branchUuid)).orElse(null);

			if (branch != null) {
				if (attemptRepair) {
					boolean repaired = false;

					// get all edges for the node, language and branch
					List<HibNodeFieldContainerEdgeImpl> edges = em.createQuery(
							"select edge from nodefieldcontainer edge where edge.node = :node and edge.branch = :branch and edge.languageTag = :language",
							HibNodeFieldContainerEdgeImpl.class)
						.setParameter("node", node)
						.setParameter("branch", branch)
						.setParameter("language", languageTag)
						.getResultList();

					// get UUIDs of all initial versions of the node in the language (independent of branch)
					Set<String> initialContentsUuids = em.createQuery("select edge.contentUuid from nodefieldcontainer edge where edge.node = :node and edge.type = :type and edge.languageTag = :language", UUID.class)
						.setParameter("node", node)
						.setParameter("type", ContainerType.INITIAL)
						.setParameter("language", languageTag)
						.getResultList().stream().map(uuid -> UUIDUtil.toShortUuid(uuid)).collect(Collectors.toSet());

					// collect edges by their types
					Map<ContainerType, List<HibNodeFieldContainerEdgeImpl>> edgesByType = new HashMap<>();
					edges.forEach(edge -> {
						edgesByType.computeIfAbsent(edge.getType(), k -> new ArrayList<>()).add(edge);
					});

					// only process if we found exactly one initial Version
					Optional<HibNodeFieldContainerEdgeImpl> initialEdge = Optional.ofNullable(edgesByType.get(ContainerType.INITIAL)).filter(initials -> initials.size() == 1)
							.map(initials -> initials.get(0));

					if (initialEdge.isPresent()) {
						Set<HibNodeFieldContainer> faultyContents = new HashSet<>();
						Set<HibNodeFieldContainerEdgeImpl> faultyEdges = new HashSet<>();

						// only successors of the initial version are valid
						Predicate<HibNodeFieldContainerEdgeImpl> isValidVersion = e -> isSuccessorOf(
								e.getNodeContainer(), initialEdge.get().getNodeContainer());

						List<HibNodeFieldContainerEdgeImpl> draftEdges = edgesByType.getOrDefault(ContainerType.DRAFT,
								Collections.emptyList());
						Optional<HibNodeFieldContainerEdgeImpl> newestDraftEdge = draftEdges.stream()
								.filter(EDGE_FILTER)
								.filter(isValidVersion)
								.max(EDGE_COMPARATOR);

						for (HibNodeFieldContainerEdgeImpl draftEdge : draftEdges) {
							// all versions, which are no successors of the initial version are considered faulty and can be deleted (possibly also the version itself)
							if (!isSuccessorOf(draftEdge.getNodeContainer(), initialEdge.get().getNodeContainer())) {
								faultyContents.add(draftEdge.getNodeContainer());
								faultyEdges.add(draftEdge);
							} else if (newestDraftEdge.isPresent()) {
								// if we still have multiple draft versions, only keep the latest edge
								if (!newestDraftEdge.get().equals(draftEdge)) {
									faultyEdges.add(draftEdge);
								}
							}
						}

						List<HibNodeFieldContainerEdgeImpl> publishedEdges = edgesByType.getOrDefault(ContainerType.PUBLISHED,
								Collections.emptyList());
						Optional<HibNodeFieldContainerEdgeImpl> newestPublishedEdge = publishedEdges.stream()
								.filter(EDGE_FILTER)
								.filter(isValidVersion)
								.max(EDGE_COMPARATOR);

						for (HibNodeFieldContainerEdgeImpl publishedEdge : publishedEdges) {
							if (!isSuccessorOf(publishedEdge.getNodeContainer(), initialEdge.get().getNodeContainer())) {
								faultyContents.add(publishedEdge.getNodeContainer());
								faultyEdges.add(publishedEdge);
							} else if (newestPublishedEdge.isPresent()) {
								if (!newestPublishedEdge.get().equals(publishedEdge)) {
									faultyEdges.add(publishedEdge);
								}
							}
						}

						// remove all faulty edges
						for (HibNodeFieldContainerEdgeImpl faultyEdge : faultyEdges) {
							contentDao.removeEdge(faultyEdge);
						}

						// all possibly fauly contents can be removed, if they are no successor of any initial version
						for (HibNodeFieldContainer faulty : faultyContents) {
							if (!isSuccessorOfAny(faulty, initialContentsUuids)) {
								contentDao.delete(faulty, false);
							}
						}

						repaired = true;
					}

					InconsistencyInfo info = new InconsistencyInfo()
							.setDescription(DESCRIPTION.formatted(nodeUuid, count, type, languageTag, branch.getName()))
							.setElementUuid(nodeUuid.toString())
							.setSeverity(SEVERITY)
							.setRepairAction(REPAIR_ACTION)
							.setRepaired(repaired);
					result.addInconsistency(info);

					tx.commit();
				} else {
					InconsistencyInfo info = new InconsistencyInfo()
							.setDescription(DESCRIPTION.formatted(nodeUuid, count, type, languageTag, branch.getName()))
							.setElementUuid(nodeUuid.toString())
							.setSeverity(SEVERITY)
							.setRepairAction(REPAIR_ACTION);
					result.addInconsistency(info);
				}
			}
		}
	}
}
