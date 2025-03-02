package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_REFERENCE_UPDATED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.contentoperation.ContentKey;
import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.PersistingNodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.UUIDUtil;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

/**
 * Dao Responsible for complex recursive deletions of nodes
 */
public class NodeDeleteDaoImpl {

	private static final Logger log = LoggerFactory.getLogger(NodeDeleteDaoImpl.class);

	private final CurrentTransaction currentTransaction;
	private final ContentStorage contentStorage;
	private final PersistingNodeDao nodeDao;

	public NodeDeleteDaoImpl(CurrentTransaction currentTransaction, ContentStorage contentStorage, PersistingNodeDao nodeDao) {
		this.currentTransaction = currentTransaction;
		this.contentStorage = contentStorage;
		this.nodeDao = nodeDao;
	}

	/**
	 * Delete all nodes related to a project.
	 * @param project
	 * @param bac
	 */
	public void deleteAllFromProject(HibProject project) {
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		((HibProjectImpl) project).getHibSchemas()
				.stream()
				.flatMap(this::getVersions)
				.forEach(version -> contentDao.delete(version, project));

		((HibProjectImpl) project).getHibMicroschemas()
				.stream()
				.flatMap(this::getMicroVersions)
				.forEach(contentDao::deleteUnreferencedMicronodes);

		currentTransaction.getTx().data().getDatabaseConnector().deleteContentEdgesByProject(em(), project);

		em().createNamedQuery("nodeBranchParents.deleteAllByProject")
				.setParameter("project", project)
				.executeUpdate();

		em().createQuery("delete from node where project = :project")
				.setParameter("project", project)
				.executeUpdate();
	}

	/**
	 * Delete a node and all of its children from all branches.
	 * @param node
	 * @param bac
	 * @param ignoreChecks
	 */
	public void deleteRecursive(HibNode node, boolean ignoreChecks) {
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		Set<HibNodeImpl> descendants = em().createNamedQuery("nodeBranchParents.findDescendants", HibNodeImpl.class)
				.setParameter("node", node)
				.getResultStream()
				.collect(Collectors.toSet());

		List<UUID> nodesUuidsToDelete = descendants.stream().map(HibNodeImpl::getDbUuid).collect(Collectors.toList());
		Set<HibSchemaVersion> versions = descendants.stream().map(HibNode::getSchemaContainer)
				.flatMap(this::getVersions)
				.collect(Collectors.toSet());

		// Send Node Updated events
		addNodeUpdateReferenceEvents(nodesUuidsToDelete);

		contentDao.delete(versions, descendants);

		descendants.forEach(n -> HibernateTx.get().batch().add(onDeleted(n)));

		// clear entity manager before performing bulk delete queries
		em().clear();

		// note: we need to use half the split-size here, because at least one of the queries will inject the slice twice.
		SplittingUtils.splitAndConsume(nodesUuidsToDelete, HibernateUtil.inQueriesLimitForSplitting(1) / 2, slice -> {
			currentTransaction.getTx().data().getDatabaseConnector().deleteContentEdgesByUuids(em(), slice);

			em().createNamedQuery("nodeBranchParents.deleteAllByNodeUuids")
					.setParameter("nodesUuid", slice)
					.executeUpdate();

			em().createNamedQuery("nodelistitem.deleteByNodeUuids")
					.setParameter("nodeUuids", slice)
					.executeUpdate();

			em().createNamedQuery("nodefieldref.deleteEdgeByNodeUuids")
					.setParameter("uuids", slice)
					.executeUpdate();

			em().createQuery("delete from node where dbUuid in :nodesUuid")
					.setParameter("nodesUuid", slice)
					.executeUpdate();
		});
		CommonTx.get().data().maybeGetBulkActionContext().ifPresent(BulkActionContext::process);
	}

	/**
	 * Delete a node and all of its children from the provided branch.
	 * @param node
	 * @param branch
	 * @param bac
	 * @param ignoreChecks
	 */
	public void deleteRecursiveFromBranch(HibNode node, HibBranch branch, boolean ignoreChecks, boolean recursive) {
		// 0. Dumb checks
		if (!ignoreChecks) {
			// Prevent deletion of basenode
			if (node.getProject().getBaseNode().getUuid().equals(node.getUuid())) {
				throw error(METHOD_NOT_ALLOWED, "node_basenode_not_deletable");
			}
			// Prevent deletion of node parent, if not recursive
			if (!recursive) {
				if (nodeDao.getChildren(node, branch.getUuid()).hasNext()) {
					throw error(BAD_REQUEST, "node_error_delete_failed_node_has_children");
				}
			}
		}

		// 1. get descendants in branch, fetching the edges as well
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		EntityGraph<?> entityGraph = em().getEntityGraph("node.content");
		Set<HibNodeImpl> descendants = em().createNamedQuery("nodeBranchParents.findDescendantsInBranch", HibNodeImpl.class)
				.setParameter("node", node)
				.setParameter("branch", branch)
				.setHint("jakarta.persistence.fetchgraph", entityGraph)
				.getResultStream()
				.collect(Collectors.toSet());
		List<UUID> nodesUuidsToDelete = descendants.stream().map(HibNodeImpl::getDbUuid).collect(Collectors.toList());
		Set<HibSchemaVersion> versions = descendants.stream().map(HibNode::getSchemaContainer)
				.flatMap(this::getVersions)
				.collect(Collectors.toSet());
		Set<ContentKey> relatedContainers = new HashSet<>();
		versions.forEach(version -> {
			// Get the containers related to the descendants. We get only the content keys to get memory consumption low
			List<ContentKey> containersUuuids = contentStorage.findByNodes(version, descendants);
			relatedContainers.addAll(containersUuuids);
		});
		// relatedContainers might contain containers which belongs to other branches, we need to figure out only the
		// ones belonging to the provided branch that are not referenced in other branches
		Set<ContentKey> deletableContainersForBranch = findDeletableContainersForBranch(relatedContainers, descendants, branch);
		// before deleting the containers, we must submit reference updates events
		addNodeUpdateReferenceEvents(nodesUuidsToDelete);
		// clear entity manager before performing bulk queries
		em().clear();
		// delete the containers
		contentDao.delete(deletableContainersForBranch);
		// note: we need to use half the split-size here, because at least one of the queries will inject the slice twice.
		List<HibNodeImpl> deletableNodes = SplittingUtils.splitAndMergeInList(nodesUuidsToDelete, HibernateUtil.inQueriesLimitForSplitting(2) / 2, slice -> {
			// delete edges
			currentTransaction.getTx().data().getDatabaseConnector().deleteContentEdgesByBranchUuids(em(), branch, slice);

			// delete parent branches
			em().createNamedQuery("nodeBranchParents.deleteAllByNodeUuidsAndBranch")
					.setParameter("nodesUuid", slice)
					.setParameter("branchUuid", branch.getId())
					.executeUpdate();

			// delete the nodes which don't have any more edges
			return em().createQuery("select n from node n where n.dbUuid in :nodesUuid and size(n.content) = 0", HibNodeImpl.class)
				.setParameter("nodesUuid", slice)
				.getResultList();
		});
		// create delete events
		deletableNodes.forEach(n -> {
			HibernateTx.get().batch().add(onDeleted(n));
			em().detach(n); // make sure the elements are not stored in the persistence context, otherwise em().find() will still return them after deletion
		});
		// finally delete the nodes
		SplittingUtils.splitAndConsume(deletableNodes.stream().map(HibNode::getId).collect(Collectors.toList()), HibernateUtil.inQueriesLimitForSplitting(0), slice -> em().createQuery("delete from node where dbUuid in :nodesUuid")
				.setParameter("nodesUuid", slice)
				.executeUpdate());
	}

	private Set<ContentKey> findDeletableContainersForBranch(Set<ContentKey> containersKeys, Set<HibNodeImpl> descendants, HibBranch branch) {
		Map<UUID, Set<UUID>> branchMap = getBranchesOfContents(containersKeys.stream().map(ContentKey::getContentUuid).collect(Collectors.toSet()));
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		Map<ContentKey, ContentKey> previousContainersUuidsMap = contentDao.getPreviousContainersUuidsMap(containersKeys);
		Map<ContentKey, List<ContentKey>> nextContainersUuidsMap = previousContainersUuidsMap.entrySet()
				.stream()
				.filter(es -> es.getValue() != null)
				.collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

		List<HibNodeFieldContainerEdgeImpl> draftEdgesForBranch = descendants.stream()
				.flatMap(node -> node.getContentEdges().stream())
				.filter(edge -> edge.getType().equals(ContainerType.DRAFT) && edge.getBranch().equals(branch))
				.collect(Collectors.toList());

		// We now traverse the versions chain backwards, starting from the draft edges.
		// All the containersKeys that do not have edges in other branches can be removed.
		Set<ContentKey> containersKeysToBeRemoved = new HashSet<>();

		draftEdgesForBranch.forEach(edge -> {
			ContentKey current = ContentKey.fromContentUUIDAndVersion(edge.getContentUuid(), edge.getVersion());

			while (current != null) {
				Set<UUID> branches = branchMap.getOrDefault(current.getContentUuid(), Collections.emptySet());
				if (branches.size() > 1) {
					break;
				}

				List<ContentKey> nextVersions = nextContainersUuidsMap.getOrDefault(current, new ArrayList<>());
				if (nextVersions.size() > 1) {
					// we have found a container that is referenced in two branches
					// stop traversal
					break;
				}

				containersKeysToBeRemoved.add(current);
				current = previousContainersUuidsMap.get(current);
			}
		});

		return containersKeysToBeRemoved;
	}

	/**
	 * For the given set of content UUIDs, collect the branch UUIDs in which the contents are used
	 * @param contentUuids set of content UUIDs
	 * @return map of content UUID to set of branch UUIDs
	 */
	private Map<UUID, Set<UUID>> getBranchesOfContents(Set<UUID> contentUuids) {
		Map<UUID, Set<UUID>> ret = new HashMap<>();

		SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(0), slice -> {
			em()
				.createQuery("select contentUuid, branch from nodefieldcontainer where contentUuid in :uuids", Tuple.class)
				.setParameter("uuids", slice)
				.getResultList()
				.forEach(tuple -> {
					UUID contentUuid = tuple.get(0, UUID.class);
					UUID branchUuid = UUIDUtil.toJavaUuid(tuple.get(1, HibBranch.class).getUuid());
					ret.computeIfAbsent(contentUuid, key -> new HashSet<>()).add(branchUuid);
				});
		});

		return ret;
	}

	private void addNodeUpdateReferenceEvents(List<UUID> nodesUuidsToDelete) {
		Set<String> handledNodeUuids = new HashSet<>();
		// node reference updates events for nodes and node list referencing the to be deleted nodes.
		addNodeUpdateReferenceEventsForReferencingFields(nodesUuidsToDelete, handledNodeUuids);
		addNodeUpdateReferenceEventsForReferencingListItems(nodesUuidsToDelete, handledNodeUuids);
		// node reference updates events for micronodes and micronodes list referencing the to be deleted nodes.
		List<UUID> micronodesReferencingNodes = micronodesReferencingNodes(nodesUuidsToDelete);
		addNodeUpdateReferenceEventsForReferencingMicroFields(micronodesReferencingNodes, handledNodeUuids);
		addNodeUpdateReferenceEventsForReferencingMicroListItems(micronodesReferencingNodes, handledNodeUuids);
	}

	private void addNodeUpdateReferenceEventsForReferencingFields(List<UUID> nodesUuidsToDelete, Set<String> handledNodeUuids) {
		SplittingUtils.splitAndConsume(nodesUuidsToDelete, HibernateUtil.inQueriesLimitForSplitting(3), slice -> em().createQuery(
				"select e.languageTag, e.branch.dbUuid, e.type, p, c, n.dbUuid from nodefieldcontainer e " +
						" join nodefieldref ref on e.contentUuid = ref.containerUuid " +
						" join e.node n " +
						" join n.project p " +
						" join n.schemaContainer c " +
						" where ref.valueOrUuid in :nodeUuids " +
						" and (e.type = 'DRAFT' or e.type = 'PUBLISHED')", Tuple.class)
				.setParameter("nodeUuids", slice)
				.getResultList()
				.forEach(tuple -> addReferenceUpdateEvent(handledNodeUuids, tuple)));
	}

	private void addNodeUpdateReferenceEventsForReferencingListItems(List<UUID> nodesUuidsToDelete, Set<String> handledNodeUuids) {
		SplittingUtils.splitAndConsume(nodesUuidsToDelete, HibernateUtil.inQueriesLimitForSplitting(3), slice -> em().createQuery(
				"select e.languageTag, e.branch.dbUuid, e.type, p, c, n.dbUuid from nodefieldcontainer e " +
						" join nodelistitem l on e.contentUuid = l.containerUuid " +
						" join e.node n " +
						" join n.project p " +
						" join n.schemaContainer c " +
						" where l.valueOrUuid in :nodeUuids " +
						" and (e.type = 'DRAFT' or e.type = 'PUBLISHED')", Tuple.class)
				.setParameter("nodeUuids", slice)
				.getResultList()
				.forEach(tuple -> addReferenceUpdateEvent(handledNodeUuids, tuple)));
	}

	@SuppressWarnings("unchecked")
	private List<UUID> micronodesReferencingNodes(List<UUID> nodesUuidsToDelete) {
		return SplittingUtils.splitAndMergeInList(nodesUuidsToDelete, HibernateUtil.inQueriesLimitForSplitting(2), slice -> {
			List<UUID> micronodesReferencingNodeInField = em().createQuery(
							"select containerUuid from nodefieldref ref " +
									" where ref.valueOrUuid in :nodeUuids " +
									" and ref.containerType = :containerType")
					.setParameter("nodeUuids", slice)
					.setParameter("containerType", ReferenceType.MICRONODE)
					.getResultList();
		
			List<UUID> micronodesReferencingNodeInList = em().createQuery(
							"select containerUuid from nodelistitem ref " +
									" where ref.valueOrUuid in :nodeUuids " +
									" and ref.containerType = :containerType")
					.setParameter("nodeUuids", slice)
					.setParameter("containerType", ReferenceType.MICRONODE)
					.getResultList();

			return Stream.of(micronodesReferencingNodeInField, micronodesReferencingNodeInList).flatMap(Collection::stream).collect(Collectors.toList());
		});
	}

	private void addNodeUpdateReferenceEventsForReferencingMicroFields(List<UUID> micronodesReferencingNodes, Set<String> handledNodeUuids) {
		SplittingUtils.splitAndConsume(micronodesReferencingNodes, HibernateUtil.inQueriesLimitForSplitting(3), slice -> em().createQuery(
					"select e.languageTag, e.branch.dbUuid, e.type, p, c, n.dbUuid from nodefieldcontainer e " +
							" join micronodefieldref ref on e.contentUuid = ref.containerUuid " +
							" join e.node n " +
							" join n.project p " +
							" join n.schemaContainer c " +
							" where ref.valueOrUuid in :micronodeUuids " +
							" and (e.type = 'DRAFT' or e.type = 'PUBLISHED')", Tuple.class)
					.setParameter("micronodeUuids", slice)
					.getResultList()
					.forEach(tuple -> addReferenceUpdateEvent(handledNodeUuids, tuple)));
	}

	private void addNodeUpdateReferenceEventsForReferencingMicroListItems(List<UUID> micronodesReferencingNodes, Set<String> handledNodeUuids) {
		SplittingUtils.splitAndConsume(micronodesReferencingNodes, HibernateUtil.inQueriesLimitForSplitting(3), slice -> em().createQuery(
				"select e.languageTag, e.branch.dbUuid, e.type, p, c, n.dbUuid from nodefieldcontainer e " +
						" join micronodelistitem ref on e.contentUuid = ref.containerUuid " +
						" join e.node n " +
						" join n.project p " +
						" join n.schemaContainer c " +
						" where ref.valueOrUuid in :micronodeUuids " +
						" and (e.type = 'DRAFT' or e.type = 'PUBLISHED')", Tuple.class)
				.setParameter("micronodeUuids", slice)
				.getResultList()
				.forEach(tuple -> addReferenceUpdateEvent(handledNodeUuids, tuple)));
	}

	private NodeMeshEventModel onDeleted(HibNode node) {
		NodeMeshEventModel event = new NodeMeshEventModel();
		event.setEvent(node.getTypeInfo().getOnDeleted());
		event.setUuid(node.getUuid());
		event.setLanguageTag(null);
		event.setType(null);
		event.setBranchUuid(null);
		event.setProject(node.getProject().transformToReference());
		HibSchema schema = node.getSchemaContainer();
		if (schema != null) {
			event.setSchema(schema.transformToReference());
		}
		return event;
	}

	private void addReferenceUpdateEvent(Set<String> handledNodeUuids, Tuple tuple) {
		String languageTag = (String) tuple.get(0);
		UUID branchUuid = (UUID) tuple.get(1);
		ContainerType ctype = (ContainerType) tuple.get(2);
		HibProjectImpl project = (HibProjectImpl) tuple.get(3);
		HibSchema schema = (HibSchema) tuple.get(4);
		UUID uuid = (UUID) tuple.get(5);
		addReferenceUpdateEvent(handledNodeUuids, UUIDUtil.toShortUuid(uuid), schema, languageTag, UUIDUtil.toShortUuid(branchUuid), ctype, project);
	}
   
	private void addReferenceUpdateEvent(Set<String> handledNodeUuids, String referencingNodeUuid, HibSchema schema, String languageTag, String branchUuid, ContainerType type, HibProject project) {
		String key = referencingNodeUuid + languageTag + branchUuid + type.getCode();
		if (!handledNodeUuids.contains(key)) {
			HibernateTx.get().batch().add(onReferenceUpdated(referencingNodeUuid, schema, branchUuid, type, languageTag, project));
			handledNodeUuids.add(key);
		}
	}

	private NodeMeshEventModel onReferenceUpdated(String uuid, HibSchema schema, String branchUuid, ContainerType type, String languageTag, HibProject project) {
		NodeMeshEventModel event = new NodeMeshEventModel();
		event.setEvent(NODE_REFERENCE_UPDATED);
		event.setUuid(uuid);
		event.setLanguageTag(languageTag);
		event.setType(type);
		event.setBranchUuid(branchUuid);
		event.setProject(project.transformToReference());
		if (schema != null) {
			event.setSchema(schema.transformToReference());
		}
		return event;
	}

	private Stream<HibSchemaVersion> getVersions(HibSchema schema) {
		return StreamUtil.toStream(Tx.get().schemaDao().findAllVersions(schema));
	}

	private Stream<HibMicroschemaVersion> getMicroVersions(HibMicroschema schema) {
		return StreamUtil.toStream(Tx.get().microschemaDao().findAllVersions(schema));
	}


	private EntityManager em() {
		return currentTransaction.getEntityManager();
	}
}
