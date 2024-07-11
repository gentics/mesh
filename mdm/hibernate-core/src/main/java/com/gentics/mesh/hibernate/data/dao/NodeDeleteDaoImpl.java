package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_REFERENCE_UPDATED;

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
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
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

	public NodeDeleteDaoImpl(CurrentTransaction currentTransaction, ContentStorage contentStorage) {
		this.currentTransaction = currentTransaction;
		this.contentStorage = contentStorage;
	}

	/**
	 * Delete all nodes related to a project.
	 * @param project
	 * @param bac
	 */
	public void deleteAllFromProject(Project project, BulkActionContext bac) {
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		((HibProjectImpl) project).getHibSchemas()
				.stream()
				.flatMap(this::getVersions)
				.forEach(version -> contentDao.delete(version, project, bac));

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
	public void deleteRecursive(Node node, BulkActionContext bac, boolean ignoreChecks) {
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		Set<HibNodeImpl> descendants = em().createNamedQuery("nodeBranchParents.findDescendants", HibNodeImpl.class)
				.setParameter("node", node)
				.getResultStream()
				.collect(Collectors.toSet());

		List<UUID> nodesUuidsToDelete = descendants.stream().map(HibNodeImpl::getDbUuid).collect(Collectors.toList());
		Set<SchemaVersion> versions = descendants.stream().map(Node::getSchemaContainer)
				.flatMap(this::getVersions)
				.collect(Collectors.toSet());

		// Send Node Updated events
		addNodeUpdateReferenceEvents(nodesUuidsToDelete, bac);

		contentDao.delete(versions, descendants, bac);

		descendants.forEach(n -> bac.add(onDeleted(n)));

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
		bac.process();
	}

	/**
	 * Delete a node and all of its children from the provided branch.
	 * @param node
	 * @param branch
	 * @param bac
	 * @param ignoreChecks
	 */
	public void deleteRecursiveFromBranch(Node node, Branch branch, BulkActionContext bac, boolean ignoreChecks) {
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
		Set<SchemaVersion> versions = descendants.stream().map(Node::getSchemaContainer)
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
		addNodeUpdateReferenceEvents(nodesUuidsToDelete, bac);
		// clear entity manager before performing bulk queries
		em().clear();
		// delete the containers
		contentDao.delete(deletableContainersForBranch, bac);
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
			bac.add(onDeleted(n));
			em().detach(n); // make sure the elements are not stored in the persistence context, otherwise em().find() will still return them after deletion
		});
		// finally delete the nodes
		SplittingUtils.splitAndConsume(deletableNodes.stream().map(Node::getId).collect(Collectors.toList()), HibernateUtil.inQueriesLimitForSplitting(0), slice -> em().createQuery("delete from node where dbUuid in :nodesUuid")
				.setParameter("nodesUuid", slice)
				.executeUpdate());
	}

	private Set<ContentKey> findDeletableContainersForBranch(Set<ContentKey> containersKeys, Set<HibNodeImpl> descendants, Branch branch) {
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
					UUID branchUuid = UUIDUtil.toJavaUuid(tuple.get(1, Branch.class).getUuid());
					ret.computeIfAbsent(contentUuid, key -> new HashSet<>()).add(branchUuid);
				});
		});

		return ret;
	}

	private void addNodeUpdateReferenceEvents(List<UUID> nodesUuidsToDelete, BulkActionContext bac) {
		Set<String> handledNodeUuids = new HashSet<>();
		// node reference updates events for nodes and node list referencing the to be deleted nodes.
		addNodeUpdateReferenceEventsForReferencingFields(nodesUuidsToDelete, bac, handledNodeUuids);
		addNodeUpdateReferenceEventsForReferencingListItems(nodesUuidsToDelete, bac, handledNodeUuids);
		// node reference updates events for micronodes and micronodes list referencing the to be deleted nodes.
		List<UUID> micronodesReferencingNodes = micronodesReferencingNodes(nodesUuidsToDelete);
		addNodeUpdateReferenceEventsForReferencingMicroFields(micronodesReferencingNodes, bac, handledNodeUuids);
		addNodeUpdateReferenceEventsForReferencingMicroListItems(micronodesReferencingNodes, bac, handledNodeUuids);
	}

	private void addNodeUpdateReferenceEventsForReferencingFields(List<UUID> nodesUuidsToDelete, BulkActionContext bac, Set<String> handledNodeUuids) {
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
				.forEach(tuple -> addReferenceUpdateEvent(handledNodeUuids, bac, tuple)));
	}

	private void addNodeUpdateReferenceEventsForReferencingListItems(List<UUID> nodesUuidsToDelete, BulkActionContext bac, Set<String> handledNodeUuids) {
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
				.forEach(tuple -> addReferenceUpdateEvent(handledNodeUuids, bac, tuple)));
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

	private void addNodeUpdateReferenceEventsForReferencingMicroFields(List<UUID> micronodesReferencingNodes, BulkActionContext bac, Set<String> handledNodeUuids) {
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
					.forEach(tuple -> addReferenceUpdateEvent(handledNodeUuids, bac, tuple)));
	}

	private void addNodeUpdateReferenceEventsForReferencingMicroListItems(List<UUID> micronodesReferencingNodes, BulkActionContext bac, Set<String> handledNodeUuids) {
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
				.forEach(tuple -> addReferenceUpdateEvent(handledNodeUuids, bac, tuple)));
	}

	private NodeMeshEventModel onDeleted(Node node) {
		NodeMeshEventModel event = new NodeMeshEventModel();
		event.setEvent(node.getTypeInfo().getOnDeleted());
		event.setUuid(node.getUuid());
		event.setLanguageTag(null);
		event.setType(null);
		event.setBranchUuid(null);
		event.setProject(node.getProject().transformToReference());
		Schema schema = node.getSchemaContainer();
		if (schema != null) {
			event.setSchema(schema.transformToReference());
		}
		return event;
	}

	private void addReferenceUpdateEvent(Set<String> handledNodeUuids, BulkActionContext bac, Tuple tuple) {
		String languageTag = (String) tuple.get(0);
		UUID branchUuid = (UUID) tuple.get(1);
		ContainerType ctype = (ContainerType) tuple.get(2);
		HibProjectImpl project = (HibProjectImpl) tuple.get(3);
		Schema schema = (Schema) tuple.get(4);
		UUID uuid = (UUID) tuple.get(5);
		addReferenceUpdateEvent(handledNodeUuids, UUIDUtil.toShortUuid(uuid), schema, languageTag, UUIDUtil.toShortUuid(branchUuid), ctype, bac, project);
	}
   
	private void addReferenceUpdateEvent(Set<String> handledNodeUuids, String referencingNodeUuid, Schema schema, String languageTag, String branchUuid, ContainerType type, BulkActionContext bac, Project project) {
		String key = referencingNodeUuid + languageTag + branchUuid + type.getCode();
		if (!handledNodeUuids.contains(key)) {
			bac.add(onReferenceUpdated(referencingNodeUuid, schema, branchUuid, type, languageTag, project));
			handledNodeUuids.add(key);
		}
	}

	private NodeMeshEventModel onReferenceUpdated(String uuid, Schema schema, String branchUuid, ContainerType type, String languageTag, Project project) {
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

	private Stream<SchemaVersion> getVersions(Schema schema) {
		return StreamUtil.toStream(Tx.get().schemaDao().findAllVersions(schema));
	}

	private Stream<MicroschemaVersion> getMicroVersions(Microschema schema) {
		return StreamUtil.toStream(Tx.get().microschemaDao().findAllVersions(schema));
	}


	private EntityManager em() {
		return currentTransaction.getEntityManager();
	}
}
