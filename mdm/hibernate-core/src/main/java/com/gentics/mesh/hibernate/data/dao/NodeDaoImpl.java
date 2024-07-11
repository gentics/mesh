package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.hibernate.util.HibernateUtil.collectVersionColumns;
import static com.gentics.mesh.hibernate.util.HibernateUtil.firstOrNull;
import static com.gentics.mesh.hibernate.util.HibernateUtil.inQueriesLimitForSplitting;
import static com.gentics.mesh.hibernate.util.HibernateUtil.makeAlias;
import static com.gentics.mesh.hibernate.util.HibernateUtil.makeParamName;
import static com.gentics.mesh.hibernate.util.HibernateUtil.streamContentSelectClause;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import com.gentics.graphqlfilter.filter.operation.Combiner;
import com.gentics.graphqlfilter.filter.operation.Comparison;
import com.gentics.graphqlfilter.filter.operation.FieldOperand;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.graphqlfilter.filter.operation.LiteralOperand;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.contentoperation.ContentColumn;
import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.contentoperation.JoinedContentColumn;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.CoreElement;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.NodeFieldContainerEdge;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.PersistingNodeDao;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.HibernateTxImpl;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.ContentInterceptor;
import com.gentics.mesh.hibernate.data.domain.HibBranchImpl;
import com.gentics.mesh.hibernate.data.domain.HibBranchNodeParent;
import com.gentics.mesh.hibernate.data.domain.HibBranchNodeParentId;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;
import com.gentics.mesh.hibernate.data.domain.misc.NodeData;
import com.gentics.mesh.hibernate.data.loader.DataLoaders;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateFilter;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.JpaUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.parameter.DeleteParameters;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.query.NativeJoin;
import com.gentics.mesh.query.ReferencedNodesFilterJoin;
import com.gentics.mesh.unhibernate.Select;
import com.gentics.mesh.util.CollectionUtil;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.vertx.core.Vertx;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Node DAO implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class NodeDaoImpl extends AbstractHibRootDao<Node, NodeResponse, HibNodeImpl, Project, HibProjectImpl> implements PersistingNodeDao {
	private final ContentStorage contentStorage;
	private final NodeDeleteDaoImpl nodeDeleteDao;
	private final DatabaseConnector databaseConnector;

	@Inject
	public NodeDaoImpl(RootDaoHelper<Node, HibNodeImpl, Project, HibProjectImpl> rootDaoHelper,
					   HibPermissionRoots permissionRoots, DatabaseConnector databaseConnector,
					   CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory,
					   Lazy<Vertx> vertx, ContentStorage contentStorage) {
		super(rootDaoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
		this.contentStorage = contentStorage;
		this.databaseConnector = databaseConnector;
		this.nodeDeleteDao = new NodeDeleteDaoImpl(currentTransaction, contentStorage);
	}

	@Override
	public String mapGraphQlFilterFieldName(String gqlName) {
		switch (gqlName) {
		case "fieldName": return ReferencedNodesFilterJoin.ALIAS_CONTENTFIELDKEY;
		case "micronodeFieldName": return ReferencedNodesFilterJoin.ALIAS_MICROCONTENTFIELDKEY;
		case "schema": return "schemaContainer_dbUuid";
		case "creator": return "creator_dbUuid";
		case "edited": case "CONTENT.edited": return "CONTAINER.edited";
		case "editor": case "CONTENT.editor": return "CONTAINER.editor_dbUuid";
		}
		if (gqlName.endsWith("references")) {
			return super.mapGraphQlFilterFieldName("uuid");
		}
		return super.mapGraphQlFilterFieldName(gqlName);
	}

	@Override
	public String mapGraphQlSortingFieldName(String gqlName) {
		switch (gqlName) {
		case "references": return super.mapGraphQlSortingFieldName("uuid");
		case "editor": return "CONTAINER.editor_dbUuid";
		case "schema": return "SCHEMA.schemacontainer_dbUuid";
		case "CONTAINER.editor_dbUuid": return super.mapGraphQlSortingFieldName("editor");
		}
		return super.mapGraphQlSortingFieldName(gqlName);
	}

	@Override
	public Function<Node, Project> rootGetter() {
		return Node::getProject;
	}

	@Override
	public Result<? extends Node> getChildren(Node node) {
		return new TraversalResult<>(
				em().createNamedQuery("nodeBranchParent.findChildrenByNode", HibNodeImpl.class)
						.setParameter("node", node)
						.getResultList());
	}

	@Override
	public Result<? extends Node> getChildren(Node node, String branchUuid) {
		if (maybeChildrenLoader().isPresent()) {
			return new TraversalResult<>(maybeChildrenLoader().get().apply(node));
		}

		return new TraversalResult<>(
				em().createNamedQuery("nodeBranchParent.findChildrenByNodeAndBranch", HibNodeImpl.class)
						.setParameter("node", node.getId())
						.setParameter("branch", UUIDUtil.toJavaUuid(branchUuid))
						.getResultList());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<Node, List<Node>> getChildren(Collection<Node> nodes, String branchUuid) {
		if (nodes.isEmpty()) {
			return Collections.emptyMap();
		}

		UUID branchId = UUIDUtil.toJavaUuid(branchUuid);
		List<UUID> nodesUuids = nodes.stream().map(Node::getId).map(UUID.class::cast).collect(Collectors.toList());
		Map<Node, List<Node>> map = SplittingUtils.splitAndMergeInMapOfLists(nodesUuids, inQueriesLimitForSplitting(1), (uuids) -> {
			List<Object[]> resultList = em().createNamedQuery("nodeBranchParents.findChildrenByNodesAndBranch")
					.setParameter("nodesUuids", uuids)
					.setParameter("branch", branchId)
					.getResultList();

			return resultList.stream()
					.map(tuples -> Pair.of((Node) tuples[0], (Node) tuples[1]))
					.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
		});
		return CollectionUtil.addFallbackValueForMissingKeys(map, nodes, Collections.emptyList());
	}

	@Override
	public Node getParentNode(Node node, String branchUuid) {
		if (maybeParentLoader().isPresent()) {
			return maybeParentLoader().get().apply(node);
		}

		return firstOrNull(
				em().createNamedQuery("nodeBranchParent.findParentNodeByNodeAndBranch", HibNodeImpl.class)
					.setParameter("node", node.getId())
					.setParameter("branch", UUIDUtil.toJavaUuid(branchUuid)));
	}

	@Override
	@SuppressWarnings("unchecked")
	public String getParentNodeUuid(Node node, String branchUuid) {
		List<UUID> parentUuid = em().createNamedQuery("nodeBranchParent.findParentNodeUuidByNodeAndBranch")
				.setParameter("node", node.getId())
				.setParameter("branch", UUIDUtil.toJavaUuid(branchUuid))
				.getResultList();

		if (parentUuid.isEmpty()) {
			return null;
		}

		return parentUuid.get(0).toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Node, Node> getParentNodes(Collection<Node> nodes, String branchUuid) {
		if (nodes.isEmpty()) {
			return Collections.emptyMap();
		}

		List<UUID> nodesUuids = nodes.stream().map(Node::getId).map(UUID.class::cast).collect(Collectors.toList());

		Map<Node, Node> map = SplittingUtils.splitAndMergeInMap(nodesUuids, inQueriesLimitForSplitting(1), (uuids) -> {
			List<Object[]> resultList = em().createNamedQuery("nodeBranchParents.findParentNodesByNodesAndBranch")
					.setParameter("nodesUuids", uuids)
					.setParameter("branch", UUIDUtil.toJavaUuid(branchUuid))
					.getResultList();

			return resultList.stream()
					.map(tuples -> Pair.of((Node) tuples[0], (Node) tuples[1]))
					.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
		});

		return CollectionUtil.addFallbackValueForMissingKeys(map, nodes,null);
	}

	@Override
	public void removeParent(Node node, String branchUuid) {
		em().createNamedQuery("nodeBranchParents.deleteParentsInBranch")
				.setParameter("node", node.getId())
				.setParameter("branch", UUIDUtil.toJavaUuid(branchUuid))
				.executeUpdate();
	}

	/**
	 * Set the parent node for the branch (replacing any previously set parent in the branch)
	 *
	 * @param node       child node
	 * @param branchUuid branch
	 * @param parentNode new parent node
	 */
	@Override
	public void setParentNode(Node node, String branchUuid, Node parentNode) {
		// node must be its own parent at distance 0 for the current branch
		HibBranchImpl branch = em().find(HibBranchImpl.class, UUIDUtil.toJavaUuid(branchUuid));

		boolean nodeIsNewInBranch = ensureSelfReferenceAtDepthZero(node, branch);
		ensureSelfReferenceAtDepthZero(parentNode, branch);
		if (!nodeIsNewInBranch) {
			disconnectSubTree(node, branch);
		}
		insertIntoParentTree(node, parentNode, branch);
	}

	@Override
	public void migrateParentNodes(List<? extends Node> nodes, Branch oldBranch, Branch newBranch) {
		SplittingUtils.splitAndConsume(nodes.stream().map(Node::getId).collect(Collectors.toList()), HibernateUtil.inQueriesLimitForSplitting(3), slice -> em().createNativeQuery(HibBranchNodeParent.BULK_INSERT_IN_CLOSURE_TABLE_SQL)
				.setParameter("nodeUuids", slice)
				.setParameter("newBranchUuid", newBranch.getId())
				.setParameter("oldBranchUuid", oldBranch.getId())
				.executeUpdate());
	}

	@SuppressWarnings("unchecked")
	private void disconnectSubTree(Node node, Branch branch) {
		List<UUID> children = em().createQuery("select p.child.dbUuid from node_branch_parent p " +
				"where p.nodeParent = :node and p.branchParent = :branch")
				.setParameter("node", node)
				.setParameter("branch", branch)
				.getResultList();

		if (children.isEmpty()) {
			return;
		}
		// note: we need to use half the split-size here, because at least one of the queries will inject the slice twice.
		SplittingUtils.splitAndConsume(children, HibernateUtil.inQueriesLimitForSplitting(2) / 2, slice -> em().createNamedQuery("nodeBranchParents.disconnectParentSubTree")
				.setParameter("children", slice)
				.setParameter("branch", branch)
				.executeUpdate());
	}

	private void insertIntoParentTree(Node node, Node parentNode, Branch branch) {
		em().createNativeQuery(HibBranchNodeParent.INSERT_IN_CLOSURE_TABLE_SQL)
				.setParameter("node", node.getId())
				.setParameter("parentNode", parentNode.getId())
				.setParameter("branch", branch.getId())
				.unwrap(NativeQuery.class)
				.addSynchronizedEntityClass(HibBranchNodeParent.class)
				.executeUpdate();
	}

	/**
	 * If the node is not its parent in the given branch, add it as its parent.
	 * @param node
	 * @param branch
	 * @return true if the self reference was added, false otherwise
	 */
	private boolean ensureSelfReferenceAtDepthZero(Node node, Branch branch) {
		HibBranchNodeParentId nodeSelfReferenceId = new HibBranchNodeParentId((UUID) node.getId(), (UUID) node.getId(), (UUID) branch.getId());
		if (em().find(HibBranchNodeParent.class, nodeSelfReferenceId) == null) {
			HibBranchNodeParent selfReference = new HibBranchNodeParent(node, node, branch, 0);
			em().persist(selfReference);
			return true;
		}

		return false;
	}

	@Override
	public Page<? extends Node> getChildren(Node node, InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type, PagingParameters pagingParameter) {
		CriteriaQuery<HibNodeImpl> query = cb().createQuery(HibNodeImpl.class);
		Root<HibBranchNodeParent> root = query.from(HibBranchNodeParent.class);
		query.select(root.get("child"));
		daoHelper.addRestrictions(query,
				cb().equal(root.get("nodeParent").get("dbUuid"), UUIDUtil.toJavaUuid(node.getUuid())),
				cb().equal(root.get("id").get("branchParentUuid"), UUIDUtil.toJavaUuid(branchUuid)),
				cb().equal(root.get("distance"), 1));

		Subquery<UUID> edgeQuery = query.subquery(UUID.class);
		Root<HibNodeFieldContainerEdgeImpl> edgeRoot = edgeQuery.from(HibNodeFieldContainerEdgeImpl.class);
		edgeQuery.select(edgeRoot.get("node").get("dbUuid"));

		daoHelper.addRestrictions(edgeQuery,
				cb().equal(edgeRoot.get("type"), type),
				cb().equal(edgeRoot.get("branch").get("dbUuid"), UUIDUtil.toJavaUuid(branchUuid))
		);
		if (CollectionUtils.isNotEmpty(languageTags)) {
			daoHelper.addRestrictions(edgeQuery, cb().in(edgeRoot.get("languageTag")).value(languageTags));
		}
		daoHelper.addRestrictions(query, cb().in(root.get("id").get("childUuid")).value(edgeQuery));

		List<InternalPermission> perm = type == PUBLISHED ? Arrays.asList(READ_PUBLISHED_PERM, READ_PERM) : Collections.singletonList(READ_PERM);
		daoHelper.addPermissionRestriction(query, root.get("child"), ac.getUser(), perm);
		daoHelper.addPermissionRestriction(query, root.get("nodeParent"), ac.getUser(), perm);

		return daoHelper.getResultPage(ac, query, pagingParameter, null, null);
	}

	@Override
	public Stream<? extends Node> getChildrenStream(Node node, InternalActionContext ac, InternalPermission perm) {
		Tx tx = Tx.get();
		Branch branch = tx.getBranch(ac);
		CriteriaQuery<HibNodeImpl> query = cb().createQuery(HibNodeImpl.class);
		Root<HibBranchNodeParent> root = query.from(HibBranchNodeParent.class);
		query.select(root.get("child"));
		daoHelper.addRestrictions(query,
				cb().equal(root.get("nodeParent").get("dbUuid"), UUIDUtil.toJavaUuid(node.getUuid())),
				cb().equal(root.get("branchParent"), branch),
				cb().equal(root.get("distance"), 1));

		List<InternalPermission> perms = perm == READ_PUBLISHED_PERM ? Arrays.asList(READ_PUBLISHED_PERM, READ_PERM) : Collections.singletonList(READ_PERM);
		daoHelper.addPermissionRestriction(query, root.get("child"), ac.getUser(), perms);

		return em().createQuery(query).getResultStream();
	}

	@Override
	public Map<Node, List<NodeContent>> getChildren(Set<Node> nodes, InternalActionContext ac, String branchUuid, List<String> languageTags, ContainerType type, PagingParameters paging, Optional<FilterOperation<?>> maybeNativeFiltering) {
		InternalPermission perm = type == PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM;
		Map<Node, List<Node>> nodeChildrenMap = Optional.ofNullable(maybeNativeFiltering.isPresent()).filter(b -> b)
				.map(unused -> findAllStream(Tx.get().getProject(ac), ac, perm, paging, 
								Optional.ofNullable(type), maybeNativeFiltering, Optional.ofNullable(nodes), Optional.empty(), Optional.empty(), Optional.ofNullable(languageTags), Optional.of(UUIDUtil.toJavaUuid(branchUuid)), true)
						.collect(Collectors.groupingBy(p -> p.getParentEdge().getNodeParent(), Collectors.mapping(p -> p.getNode(), Collectors.toList()))))
				.orElseGet(() -> {
					Map<Node, List<Node>> children = getChildren(nodes, branchUuid);

					// filter for permissions
					User user = ac.getUser();
					if (!user.isAdmin()) {
						// prepare permissions for all nodes
						PersistingUserDao userDao = CommonTx.get().userDao();
						List<Object> nodeIds = children.values().stream().flatMap(Collection::stream).map(Node::getId).collect(Collectors.toList());
						userDao.preparePermissionsForElementIds(user, nodeIds);
						// remove children without permission
						children.values().forEach(nodeList -> nodeList.removeIf(node -> !userDao.hasPermission(user, node, perm)));
					}
					return children;
				});

		if (nodeChildrenMap.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<Node> allNodes = nodeChildrenMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());

		Map<UUID, NodeFieldContainer> fieldContainers = SplittingUtils.splitAndMergeInMap(allNodes, inQueriesLimitForSplitting(3), (nodesSlice) -> getFieldContainers(Collections.emptyList(), nodesSlice, languageTags, branchUuid, type));
		Map<Node, List<NodeContent>> map = nodeChildrenMap.entrySet().stream()
				.map(kv -> {
					List<NodeContent> content = kv.getValue().stream().distinct().map(n -> new NodeContent(n, fieldContainers.getOrDefault(n.getId(), null), languageTags, type)).collect(Collectors.toList());
					return Pair.of(kv.getKey(), content);
				}).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
		return CollectionUtil.addFallbackValueForMissingKeys(map, nodes, Collections.emptyList());
	}

	@Override
	public boolean isBaseNode(Node node) {
		return node.isBaseNode();
	}

	@Override
	public void removeElement(Node node) {
		em().remove(node);
	}

	@Override
	public Node findByName(Project project, String name) {
		return firstOrNull(rootDaoHelper.findByElementInRoot(project, null, "name", name, null));
	}

	@Override
	public Result<? extends Node> findAll(Project project) {
		return new TraversalResult<>(em().createNamedQuery("node.findByProject", HibNodeImpl.class)
				.setParameter("project", project)
				.getResultList().iterator());
	}

	@SuppressWarnings("unchecked")
	public Stream<UUID> findAllUuids(Project project) {
		return em().createNamedQuery("node.findUuidsByProject")
				.setParameter("project", project)
				.getResultStream();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Page<? extends Node> findAll(Project project, InternalActionContext ac, PagingParameters pagingInfo) {
		ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
		User user = ac.getUser();
		UUID branchUuid = UUIDUtil.toJavaUuid(Tx.get().getBranch(ac).getUuid());
		if (PersistingRootDao.shouldSort(pagingInfo)) {
			InternalPermission perm = type == ContainerType.PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM;
			return new PageImpl<Node>(
					findAllStream(project, ac, perm, pagingInfo, Optional.of(type), Optional.empty()).collect(Collectors.toList()), 
					pagingInfo, 
					countAll(project, ac, perm, pagingInfo, Optional.of(type), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(branchUuid), false));
		}
		List<? extends Node> result;
		long totalElements;
		if (user.isAdmin()) {
			Query<HibNodeImpl> query = em().createNamedQuery("node.findByProjectBranchAndContainerType.admin", HibNodeImpl.class)
					.unwrap(Query.class)
					.setParameter("project", project)
					.setParameter("branchUuid", branchUuid)
					.setParameter("type", type);
			result = getResult(query, pagingInfo);
			totalElements = ((Number) em().createQuery(JpaUtil.toCountHQL(query.getQueryString()))
					.setParameter("project", project)
					.setParameter("branchUuid", branchUuid)
					.setParameter("type", type)
					.getSingleResult()).longValue();
		} else if (type == PUBLISHED) {
			List<Role> roles = IteratorUtils.toList(Tx.get().userDao().getRoles(user).iterator());
			Query<HibNodeImpl> query = em().createNamedQuery("node.findByProjectBranchAndContainerType.read_published", HibNodeImpl.class).unwrap(Query.class);
			result = SplittingUtils.splitAndMergeInList(roles, HibernateUtil.inQueriesLimitForSplitting(3), slice -> {
				query.setParameter("project", project)
					.setParameter("branchUuid", branchUuid)
					.setParameter("type", type)
						.setParameter("roles", slice)
					.unwrap(Query.class);
				return getResult(query, pagingInfo);
			});			
			totalElements = SplittingUtils.splitAndCount(roles, HibernateUtil.inQueriesLimitForSplitting(3), slice -> ((Number) em().createQuery(JpaUtil.toCountHQL(query.getQueryString()))
								.setParameter("project", project)
								.setParameter("branchUuid", branchUuid)
								.setParameter("type", type)
					.setParameter("roles", slice)
					.getSingleResult()).longValue());
		} else {
			List<Role> roles = IteratorUtils.toList(Tx.get().userDao().getRoles(user).iterator());
			Query<HibNodeImpl> query  = em().createNamedQuery("node.findByProjectBranchAndContainerType.read", HibNodeImpl.class).unwrap(Query.class);
			result = SplittingUtils.splitAndMergeInList(roles, HibernateUtil.inQueriesLimitForSplitting(3), slice -> {
				query.setParameter("project", project)
					.setParameter("branchUuid", branchUuid)
					.setParameter("type", type)
						.setParameter("roles", slice)
					.unwrap(Query.class);
				return getResult(query, pagingInfo);
			});
			totalElements = SplittingUtils.splitAndCount(roles, HibernateUtil.inQueriesLimitForSplitting(3), slice -> ((Number) em().createQuery(JpaUtil.toCountHQL(query.getQueryString()))
					.setParameter("project", project)
					.setParameter("branchUuid", branchUuid)
					.setParameter("type", type)
					.setParameter("roles", slice)
					.getSingleResult()).longValue());
		}
		return new PageImpl<>(result, pagingInfo, totalElements);
	}

	private List<HibNodeImpl> getResult(Query<HibNodeImpl> query, PagingParameters pagingInfo) {
		if (pagingInfo.getPerPage() != null) {
			query.setFirstResult(pagingInfo.getActualPage() * pagingInfo.getPerPage().intValue())
					.setMaxResults(pagingInfo.getPerPage().intValue());
		}
		return query.getResultList();
	}

	@Override
	public Page<? extends Node> findAll(Project project, InternalActionContext ac, PagingParameters pagingInfo, Predicate<Node> filter) {
		return rootDaoHelper.findAllInRoot(project, ac, pagingInfo, Optional.empty(), filter, true);
	}

	@Override
	public Stream<NodeContent> findAllContent(Project project, InternalActionContext ac, List<String> languageTags, ContainerType type, PagingParameters paging, Optional<FilterOperation<?>> maybeFilter) {
		if (maybeFilter.isPresent()) {
			ContentDao contentDao = Tx.get().contentDao();
			UUID branchUuid = (UUID) Tx.get().getBranch(ac).getId();
			return findAllStream(project, ac, type == ContainerType.PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM, paging, 
						Optional.ofNullable(type), maybeFilter, Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(languageTags), Optional.of(branchUuid), true)
					.map(nodeContentParent -> new NodeContent(nodeContentParent.getNode(), 
						nodeContentParent.getContent() != null 
							? nodeContentParent.getContent() 
							: contentDao.findVersion(nodeContentParent.getNode(), 
						ac, languageTags, type), languageTags, type))
					.filter(content -> content.getContainer() != null);
		} else {
			return PersistingNodeDao.super.findAllContent(project, ac, languageTags, type, paging, maybeFilter);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Pair<Stream<NodeData>, Long> query(
			Project project, InternalActionContext ac, InternalPermission perm, PagingParameters paging, Optional<ContainerType> maybeCtype, 
			Optional<FilterOperation<?>> maybeFilter, Optional<Set<Node>> maybeParents, Optional<Schema> maybeContentSchema, 
			Optional<SchemaVersion> maybeContentSchemaVersion, Optional<List<String>> maybeContainerLanguages, Optional<UUID> maybeBranch, 
			boolean noSchemaVersionExtractionFromJoins, boolean countOnly) {
		HibernateTxImpl tx = currentTransaction.getTx();
		DatabaseConnector databaseConnector = tx.data().getDatabaseConnector();

		User user = ac.getUser();

		String nodeAlias = makeAlias(databaseConnector.maybeGetDatabaseEntityName(getPersistenceClass()).get());

		// Do we have a filter to apply?
		Optional<HibernateFilter> maybeFilterJoin = maybeFilter.map(filter -> daoHelper.appendFilter(StringUtils.EMPTY, filter, Collections.emptySet(), maybeBranch, Optional.empty(), maybeCtype, new ArrayDeque<>()));

		// Make permission table joins
		NativeJoin permJoins = daoHelper.makePermissionJoins(StringUtils.EMPTY, user, perm, maybeCtype, maybeBranch, maybeFilterJoin.map(j -> j.getRawSqlJoins()).orElse(Collections.emptySet()));

		// Make sorting table joins
		NativeJoin sortJoins = daoHelper.mapSorting(paging, StringUtils.EMPTY, maybeBranch, maybeCtype, Stream.of(
				Stream.of(permJoins),
				maybeFilterJoin.map(filterJoin -> filterJoin.getRawSqlJoins().stream()).orElse(Stream.empty())
			).flatMap(Function.identity()).collect(Collectors.toSet()));

		// One of the provided schema parameters should have a version - extract it
		maybeContentSchemaVersion = maybeContentSchemaVersion.or(() -> maybeContentSchema.map(schema -> schema.getLatestVersion()));

		// If not blocked, try extacting schema version out of filter/sort joins
		maybeContentSchemaVersion = maybeContentSchemaVersion
				.or(() -> daoHelper.extractVersion(sortJoins, noSchemaVersionExtractionFromJoins))
				.or(() -> maybeFilterJoin.flatMap(filterJoin -> filterJoin.getRawSqlJoins().stream()
						.map(join -> daoHelper.extractVersion(join, noSchemaVersionExtractionFromJoins))
						.filter(Optional::isPresent).map(Optional::get)
						.findAny()));

		// If the version is requested, collect its fields for the retrieval. Joined (binary) columns are off board.
		Optional<List<ContentColumn>> maybeContentColumns = maybeContentSchemaVersion.map(version -> collectVersionColumns(version).stream()
				.filter(c -> !JoinedContentColumn.class.isInstance(c)).collect(Collectors.toList()));

		List<String> nodeColumns = databaseConnector.getDatabaseColumnNames(getPersistenceClass()).map(columns -> columns.stream().map(column -> nodeAlias + "." + column).collect(Collectors.toList())).get();

		Select select = new Select(databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor());
		select.setTableName(databaseConnector.maybeGetPhysicalTableName(getPersistenceClass()).get() + " " + nodeAlias);
		List<String> columns = new ArrayList<>();
		columns.addAll(nodeColumns);
		maybeParents.map(parents -> makeAlias(databaseConnector.maybeGetDatabaseEntityName(HibBranchNodeParent.class).get()) + ".*").ifPresent(columns::add);
		maybeContainerLanguages.or(() -> maybeContentColumns.map(contentColumns -> Collections.emptyList())).map(yes -> makeAlias("CONTAINER") + ".*").ifPresent(columns::add);
		maybeContentColumns.ifPresent(contentColumns -> streamContentSelectClause(databaseConnector, contentColumns, Optional.of(makeAlias("CONTENT")), true).forEach(columns::add));
		if (!countOnly && PersistingRootDao.shouldSort(paging)) {
			paging.getSort().keySet().stream()
				.map(col -> maybeContainerLanguages.map(columnsExplicitlyRequested -> col).orElseGet(() -> databaseConnector.makeSortDefinition(col, Optional.empty())))
				.filter(col -> columns.stream().noneMatch(acol -> col.equalsIgnoreCase(acol)))
				.forEach(columns::add);
		}
		select.addColumn(" distinct " + columns.stream().collect(Collectors.joining(", ")));

		// Make parent edge joins, if applicable
		Optional<NativeJoin> maybeParentJoin = maybeParents.map(parents -> makeParentJoin(nodeAlias, parents, maybeBranch));

		// Make content/container join, if applicable
		Optional<NativeJoin> maybeContentJoin = maybeContentSchemaVersion.map(version -> daoHelper.makeContentJoin(StringUtils.EMPTY, permJoins, version, maybeCtype, maybeBranch, maybeContainerLanguages))
				.or(() -> maybeContainerLanguages.map(yes -> daoHelper.makeContentEdgeJoin(permJoins, StringUtils.EMPTY, maybeCtype, maybeBranch, maybeContainerLanguages)));

		// Merge joins alltogether
		Set<NativeJoin> allJoins = Stream.of(
				Stream.of(permJoins),
				maybeFilterJoin.map(filterJoin -> filterJoin.getRawSqlJoins().stream()).orElse(Stream.empty()),
				maybeParentJoin.map(filterJoin -> Stream.of(filterJoin)).orElse(Stream.empty()),
				maybeContentJoin.map(filterJoin -> Stream.of(filterJoin)).orElse(Stream.empty()),
				Stream.of(sortJoins)
			).flatMap(Function.identity())
				.filter(j -> !Objects.isNull(j))
				.collect(Collectors.toSet());

		// Install joins
		select.setJoin(daoHelper.mergeJoins(allJoins.stream()));

		// Collect wheres
		Set<String> wheres = Stream.of(
					allJoins.stream().flatMap(join -> join.getWhereClauses().stream()),
					maybeFilterJoin.filter(j -> StringUtils.isNotBlank(j.getSqlFilter())).map(j -> Stream.of(j.getSqlFilter())).orElse(Stream.empty())
				).flatMap(Function.identity()).collect(Collectors.toSet());

		// Fill wheres
		select.addRestriction(nodeAlias + "." + databaseConnector.renderNonContentColumn("project_dbUuid") + " = :project " 
				+ (wheres.size() > 0 ? wheres.stream().collect(Collectors.joining(" AND ", " AND ", StringUtils.EMPTY)) : StringUtils.EMPTY));

		// Fill sorting, if applicable
		if (!countOnly && PersistingRootDao.shouldSort(paging)) {
			String clause = paging.getSort().entrySet().stream()
					.map(entry -> maybeContainerLanguages
							.map(columnsExplicitlyRequested -> entry.getKey())
							.orElseGet(() -> databaseConnector.makeSortAlias(databaseConnector.findSortAlias(entry.getKey())) + " " + entry.getValue().getValue()))
					.collect(Collectors.joining(", "));
			if (maybeContainerLanguages.isEmpty()) {
				select.setGroupBy(" group by " + nodeColumns.stream().collect(Collectors.joining(", ")));
			}
			select.setOrderBy(" order by " + clause);
		}

		String sqlQuery = select.toStatementString();

		if (countOnly) {
			sqlQuery = databaseConnector.installCount(sqlQuery);
		} else if (PersistingRootDao.shouldPage(paging)) {
			sqlQuery = databaseConnector.installPaging(sqlQuery, nodeAlias, paging);
		}

		// Construct the query
		NativeQuery query = (NativeQuery) (countOnly ? em().createNativeQuery(sqlQuery) : em().createNativeQuery(sqlQuery, HibNodeImpl.class));
		query.setParameter("project", project.getId());

		// Add an extra parent edge entity to fetch, if applicable
		maybeParents.filter(parents -> !parents.isEmpty()).ifPresent(unused -> query.addEntity(HibBranchNodeParent.class));

		// Add an extra container edge entity to fetch, if applicable
		maybeContainerLanguages.flatMap(unused -> maybeParents).ifPresent(unused -> query.addEntity(HibNodeFieldContainerEdgeImpl.class));

		// Add an extra content entity to fetch, if applicable
		maybeContentColumns.ifPresent(contentColumns -> contentColumns.stream().forEach(c -> query.addScalar(databaseConnector.renderColumn(c), c.getJavaClass())));

		// Fill join params
		allJoins.stream().forEach(join -> {
			join.getParameters().entrySet().stream().forEach(e -> query.setParameter(e.getKey(), e.getValue()));
		});

		// Fill filter params, if applicable
		maybeFilterJoin.ifPresent(filterJoin -> filterJoin.getParameters().entrySet().stream().forEach(e -> {
			query.setParameter(e.getKey(), e.getValue());
		}));

		// Fill paging params, if applicable
		if (!countOnly && PersistingRootDao.shouldPage(paging)) {
			if (sqlQuery.contains(" :limit ")) {
				if (sqlQuery.contains(" :offset ")) {
					query.setParameter("offset", paging.getActualPage() * paging.getPerPage().intValue());
				}
				query.setParameter("limit", paging.getPerPage());
			} else {
				if (sqlQuery.contains(" :offset ")) {
					query.setParameter("offset", paging.getPerPage());
				}
			}
		}

		if (countOnly) {
			return Pair.of(Stream.empty(), daoHelper.getOrFetchTotal(query));
		} else {
			// Los geht's
			return Pair.of(query.getResultStream().map(o -> {
				// This clumsy construction is required for Hibernate, that's not being currently able to mix managed and unmanaged entities even with a help of result transformer.
				if (o.getClass().isArray() && ((Object[]) o).length > 1) {
					Object[] oo = (Object[]) o;
					// Check if node responded (normally under index 0)
					Node node = Arrays.stream(oo).filter(Node.class::isInstance).map(Node.class::cast).findAny().orElse(null);
					// Check if branch parent edge responded
					HibBranchNodeParent parentEdge = Arrays.stream(oo).filter(HibBranchNodeParent.class::isInstance).map(HibBranchNodeParent.class::cast).findAny().orElse(null);
					// Check if container edge responded
					NodeFieldContainerEdge container = Arrays.stream(oo).filter(NodeFieldContainerEdge.class::isInstance).map(NodeFieldContainerEdge.class::cast).findAny().orElse(null);
					// Check if content fields responded
					NodeFieldContainer content = maybeContentColumns.map(contentColumns -> {
						int contentIndex = (node != null ? 1 : 0) + (parentEdge != null ? 1 : 0) + (container != null ? 1 : 0);
						HibNodeFieldContainerImpl fc = new HibNodeFieldContainerImpl();
						for (int i = 0; i < contentColumns.size(); i++) {
							fc.put(contentColumns.get(i), oo[i + contentIndex]);
						}
						return fc;
					}).orElse(null);
					return new NodeData(node, parentEdge, container, content);
				} else {
					return new NodeData((Node) o, null, null, null);
				}
			}), 0L);
		}
	}

	@Override
	public long countAllContent(Project project, InternalActionContext ac, List<String> languageTags, ContainerType type, Optional<FilterOperation<?>> maybeFilter) {
		return countAll(project, ac, type == ContainerType.PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM, null, Optional.of(type), maybeFilter, 
				Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(languageTags), Optional.of((UUID) Tx.get().getBranch(ac).getId()), false);
	}

	/**
	 * Count the nodes, optionally with their parent edges, that satisfy filter, permission, paging and project dependencies.
	 * 
	 * @param project
	 * @param ac
	 * @param perm
	 * @param paging
	 * @param maybeCtype
	 * @param maybeFilter
	 * @param maybeParents  accepts an empty set as well, in this case all the parent edges will be retrieved
	 * @param maybeContentSchema
	 * @param maybeContentSchemaVersion
	 * @param maybeContainerLanguages
	 * @param maybeBranch
	 * @param noSchemaVersionExtractionFromJoins do not extract schema version from filter or sort requests, if no explicit version is provided
	 * @return
	 */
	public long countAll(
			Project project, InternalActionContext ac, InternalPermission perm, PagingParameters paging, Optional<ContainerType> maybeCtype, 
			Optional<FilterOperation<?>> maybeFilter, Optional<Set<Node>> maybeParents, Optional<Schema> maybeContentSchema, 
			Optional<SchemaVersion> maybeContentSchemaVersion, Optional<List<String>> maybeContainerLanguages, Optional<UUID> maybeBranch, boolean noSchemaVersionExtractionFromJoins) {
		return query(project, ac, perm, paging, maybeCtype, maybeFilter, maybeParents, maybeContentSchema, maybeContentSchemaVersion, maybeContainerLanguages, maybeBranch, noSchemaVersionExtractionFromJoins, true)
				.getRight();
	}

	/**
	 * Stream the nodes, optionally with their parent edges, that satisfy filter, permission, paging and project dependencies.
	 * 
	 * @param project
	 * @param ac
	 * @param perm
	 * @param paging
	 * @param maybeCtype
	 * @param maybeFilter
	 * @param maybeParents accepts an empty set as well, in this case all the parent edges will be retrieved
	 * @param maybeContentSchema
	 * @param maybeContentSchemaVersion
	 * @param maybeContainerLanguages
	 * @param maybeBranch
	 * @param noSchemaVersionExtractionFromJoins do not extract schema version from filter or sort requests, if no explicit version is provided
	 * @return
	 */
	public Stream<NodeData> findAllStream(
			Project project, InternalActionContext ac, InternalPermission perm, PagingParameters paging, Optional<ContainerType> maybeCtype, 
			Optional<FilterOperation<?>> maybeFilter, Optional<Set<Node>> maybeParents, Optional<Schema> maybeContentSchema, 
			Optional<SchemaVersion> maybeContentSchemaVersion, Optional<List<String>> maybeContainerLanguages, Optional<UUID> maybeBranch, boolean noSchemaVersionExtractionFromJoins) {
		return query(project, ac, perm, paging, maybeCtype, maybeFilter, maybeParents, maybeContentSchema, maybeContentSchemaVersion, maybeContainerLanguages, maybeBranch, noSchemaVersionExtractionFromJoins, false)
				.getLeft();
	}

	@Override
	public Stream<? extends Node> findAllStream(Project project, InternalActionContext ac, InternalPermission perm, PagingParameters paging, Optional<ContainerType> maybeContainerType, Optional<FilterOperation<?>> maybeFilter) {
		Tx tx = Tx.get();
		User user = ac.getUser();
		UUID branchUuid = (UUID) tx.getBranch(ac).getId();
		UserDao userDao = tx.userDao();

		if (PersistingRootDao.shouldSort(paging) || maybeFilter.isPresent()) {
			return findAllStream(project, ac, perm, paging, maybeContainerType, maybeFilter, 
					Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(branchUuid), true).map(nodeContentParent -> nodeContentParent.getNode());
		} else {
			return em().createNamedQuery("node.findByProject", HibNodeImpl.class)
					.setParameter("project", project)
					.getResultStream()
					.filter(o -> {
						HibNodeImpl item = HibNodeImpl.class.cast(o);
						if (perm == null || perm == READ_PUBLISHED_PERM) {
							boolean hasRead = userDao.hasPermissionForId(user, item.getId(), READ_PERM);
							if (hasRead) {
								return true;
							} else {
								// Check whether the node is published. In this case we need to check the read publish perm.
								boolean isPublishedForBranch = Long.parseLong(
										em().createNamedQuery("contentEdge.existsByNodeTypeAnBranch")
											.setParameter("node", item)
											.setParameter("type", PUBLISHED)
											.setParameter("branchUuid", branchUuid)
											.getSingleResult().toString()) > 0;
								if (isPublishedForBranch) {
									return userDao.hasPermissionForId(user, item.getId(), READ_PUBLISHED_PERM);
								}
							}
							return false;
						} else {
							return userDao.hasPermissionForId(user, item.getId(), perm);
						}
					});
		}
	}

	@Override
	public Page<? extends Node> findAllNoPerm(Project project, InternalActionContext ac,
												 PagingParameters pagingInfo) {
		return rootDaoHelper.findAllInRoot(project, ac, pagingInfo, Optional.empty(), null, false);
	}

	@Override
	public void addItem(Project root, Node item) {
		item.setProject(root);
	}

	@Override
	public void removeItem(Project root, Node item) {
		if (root.getBaseNode() != null && root.getBaseNode().getUuid().equals(item.getUuid())) {
			throw new IllegalArgumentException(
					"Cannot remove a root node { " + item.getUuid() + " } from the project { " + root.getName() + " }");
		} else {
			item.setProject(null);
		}
	}

	@Override
	public Class<? extends Node> getPersistenceClass(Project root) {
		return HibNodeImpl.class;
	}


	@Override
	public Node findByUuidGlobal(String uuid) {
		if (!UUIDUtil.isUUID(uuid)) {
			return null;
		}

		return daoHelper.findByUuid(uuid);
	}

	@Override
	public Collection<? extends Node> findByUuidGlobal(Collection<String> uuids) {
		if (CollectionUtils.isEmpty(uuids)) {
			return Collections.emptyList();
		}
		List<UUID> javaUuids = uuids.stream().filter(UUIDUtil::isUUID).map(UUIDUtil::toJavaUuid).collect(Collectors.toList());
		return loadNodesWithEdges(javaUuids);
	}

	@Override
	public long globalCount() {
		return daoHelper.count();
	}

	@Override
	public Stream<? extends Node> findAllGlobal() {
		return em().createQuery("select n from node n", HibNodeImpl.class).getResultStream();
	}

	@Override
	public void deletePersisted(Project root, Node entity) {
		beforeDeletedFromDatabase(entity);
		em().remove(entity);
		afterDeletedFromDatabase(entity);
	}

	@Override
	public Node beforeDeletedFromDatabase(Node node) {
		// clear branch parent references
		em().createNamedQuery("nodeBranchParents.deleteAllByNode")
				.setParameter("node", node)
				.executeUpdate();

		// clear referencing node list items
		em().createNamedQuery("nodelistitem.deleteByNodeUuid")
				.setParameter("nodeUuid", node.getId())
				.executeUpdate();

		//  clear referencing node fields
		em().createNamedQuery("nodefieldref.deleteEdgeByNodeUuid")
				.setParameter("uuid", node.getId())
				.executeUpdate();

		return super.beforeDeletedFromDatabase(node);
	}

	@Override
	public Node createPersisted(Project root, String uuid, Consumer<Node> inflater) {
		HibNodeImpl element = daoHelper.create(uuid, n -> {
			n.setProject(root);
			inflater.accept(n);
		});
		return afterCreatedInDatabase(element);
	}

	@Override
	public Iterator<? extends NodeFieldContainerEdge> getWebrootEdges(Node node, String segmentInfo,
																		 String branchUuid, ContainerType type) {
		return HibernateTx.get().contentDao().streamEdgesOfWebrootPath(segmentInfo, branchUuid, type).iterator();
	}

	@Override
	public Stream<NodeField> getInboundReferences(Node node, boolean lookupInFields, boolean lookupInLists) {
		HibernateTx tx = HibernateTx.get();
		EntityManager em = tx.entityManager();

		return Stream.concat(
				lookupInLists ? // Node list items
				em.createNamedQuery("nodelistitem.findByNodeUuid", HibNodeListFieldEdgeImpl.class)
						.setParameter("nodeUuid", node.getId())
						.getResultStream()
						.filter(Objects::nonNull) : Stream.empty() , 
				
				lookupInFields ? // Node fields
				em.createNamedQuery("nodefieldref.findEdgeByNodeUuid", HibNodeFieldEdgeImpl.class)
						.setParameter("uuid", node.getId())
						.getResultStream()
						.map(HibNodeFieldEdgeImpl::getField)
						.map(NodeField.class::cast) : Stream.empty());
	}

	@Override
	public Stream<NodeContent> findAllContent(Project project, InternalActionContext ac, List<String> languageTags, ContainerType type) {
		Tx tx = Tx.get();
		UUID projectId = (UUID) project.getId();
		UUID branchId = (UUID) tx.getBranch(ac).getId();
		User user = ac.getUser();

		// this will preload the nodes (and put them into the cache) for performance reasons.
		findAll(project, ac, ac.getPagingParameters());

		Optional<List<Role>> maybeRoles = Optional.ofNullable(user).filter(u -> !u.isAdmin()).map(noAdmin -> IteratorUtils.toList(Tx.get().userDao().getRoles(user).iterator()));
		Collection<NodeFieldContainer> allContent = findAllContent(maybeRoles, projectId, branchId, type, languageTags);

		return allContent.stream().map(c -> new NodeContent(c.getNode(), c, languageTags, type));
	}

	private Collection<NodeFieldContainer> findAllContent(Optional<List<Role>> maybeRoles, UUID projectId, UUID branchId, ContainerType type, List<String> languageTags) {
		List<HibNodeFieldContainerEdgeImpl> edges;
		if (maybeRoles.isEmpty()) {
			// Here we hope that the number of languages on Earth never reaches SQL parameters limit...
			edges = em().createNamedQuery("contentEdge.findByProjectAndBranchForAdmin", HibNodeFieldContainerEdgeImpl.class)
					.setParameter("projectUuid", projectId)
					.setParameter("branchUuid", branchId)
					.setParameter("type", type)
					.setParameter("languageTags", languageTags)
					.getResultList();
		} else {
			edges = SplittingUtils.splitAndMergeInList(maybeRoles.get(), HibernateUtil.inQueriesLimitForSplitting(4 + languageTags.size()), slice -> em().createNamedQuery("contentEdge.findByProjectAndBranch", HibNodeFieldContainerEdgeImpl.class)
					.setParameter("projectUuid", projectId)
					.setParameter("branchUuid", branchId)
					.setParameter("type", type)
					.setParameter("languageTags", languageTags)
					.setParameter("roles", slice)
					.getResultList());
		}

		List<HibNodeFieldContainerImpl> schemaContent = contentStorage.findMany(edges);
		return schemaContent.stream()
				.collect(collectingByNodeWithLanguageFallback(languageTags))
				.values();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Stream<NodeContent> findAllContent(SchemaVersion schemaVersion, InternalActionContext ac, List<String> languageTags, ContainerType type, PagingParameters paging, Optional<FilterOperation<?>> maybeFilter) {
		Tx tx = Tx.get();
		UUID branchId = (UUID) tx.getBranch(ac).getId();
		if (maybeFilter.isPresent()) {
			FilterOperation<?> schemaVersionFilter = Comparison.eq(new FieldOperand<>(ElementType.NODE, "schemaContainer_dbUuid", Optional.empty(), Optional.of("schemaUuid")), new LiteralOperand<>(schemaVersion.getSchemaContainer().getId(), false), StringUtils.EMPTY);
			FilterOperation allFilter = maybeFilter.map(filter -> (FilterOperation) Combiner.and(Arrays.asList(schemaVersionFilter, filter), StringUtils.EMPTY)).orElse(schemaVersionFilter);

			return findAllStream(tx.getProject(ac), ac, type == PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM, paging, 
						Optional.of(type), Optional.of(allFilter), Optional.empty(), Optional.empty(), Optional.ofNullable(schemaVersion), Optional.of(languageTags), Optional.of(branchId), true)
					.map(nc -> new NodeContent(nc.getNode(), nc.getContent(), languageTags, type));
		} else {
			User user = ac.getUser();
			List<Role> roles = user.isAdmin() ? Collections.emptyList() : IteratorUtils.toList(Tx.get().userDao().getRoles(user).iterator());
			TypedQuery<HibNodeFieldContainerEdgeImpl> edgesQuery;
			if (roles.isEmpty()) {
				edgesQuery = em().createNamedQuery("contentEdge.findByTypeBranchLanguageAndVersionForAdmin", HibNodeFieldContainerEdgeImpl.class);
			} else {
				edgesQuery = em().createNamedQuery("contentEdge.findByTypeBranchLanguageAndVersion", HibNodeFieldContainerEdgeImpl.class)
						.setParameter("roles", roles);
			}
			if (PersistingRootDao.shouldPage(paging)) {
				edgesQuery.setFirstResult(paging.getActualPage()).setMaxResults(paging.getPerPage().intValue());
			}
			List<HibNodeFieldContainerEdgeImpl> edges = edgesQuery
					.setParameter("branchUuid", branchId)
					.setParameter("type", type)
					.setParameter("languageTags", languageTags)
					.setParameter("schemaVersion", schemaVersion)
					.getResultList();		
			List<HibNodeFieldContainerImpl> schemaContent = contentStorage.findMany(edges);
		Map<UUID, NodeFieldContainer> contentByNode = schemaContent.stream()
					.collect(collectingByNodeWithLanguageFallback(languageTags));

			return contentByNode.values().stream().map(c -> new NodeContent(c.getNode(), c, languageTags, type));
		}
	}

	@Override
	public Stream<? extends Node> getBreadcrumbNodeStream(Node node, InternalActionContext ac) {
		if (maybeBreadcrumbsLoader().isPresent()) {
			return maybeBreadcrumbsLoader().get().apply(node).stream().map(HibNodeImpl.class::cast);
		}

		Branch branch = Tx.get().getBranch(ac, node.getProject());
		return em().createNamedQuery("nodeBranchParents.findBreadcrumbs", HibNodeImpl.class)
				.setParameter("node", node)
				.setParameter("branch", branch)
				.setHint(QueryHints.HINT_CACHEABLE, true)
				.getResultList().stream();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<Node, List<Node>> getBreadcrumbNodesMap(Collection<Node> nodes, InternalActionContext ac) {
		if (nodes.isEmpty()) {
			return Collections.emptyMap();
		}

		UUID branchUuid = UUIDUtil.toJavaUuid(Tx.get().getBranch(ac).getUuid());
		List<Object> nodesUuids = nodes.stream().map(Node::getId).collect(Collectors.toList());
		Map<Node, List<Node>> map = SplittingUtils.splitAndMergeInMapOfLists(nodesUuids, inQueriesLimitForSplitting(1), (uuids) -> {
			List<Object[]> list = em().createNamedQuery("nodeBranchParents.findAncestors")
					.setParameter("nodeUuids", uuids)
					.setParameter("branchUuid", branchUuid)
					.setHint(QueryHints.HINT_CACHEABLE, true)
					.getResultList();

			return list.stream()
					.map(r -> Pair.of((Node) r[0], (Node) r[1]))
					.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
		});

		return CollectionUtil.addFallbackValueForMissingKeys(map, nodes, Collections.emptyList());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void beforeGetETagForPage(Page<? extends CoreElement<? extends RestModel>> page, InternalActionContext ac) {
		ContentInterceptor contentInterceptor = HibernateTx.get().getContentInterceptor();
		List<DataLoaders.Loader> dataLoaderToInitialize = Arrays.asList(
				DataLoaders.Loader.CHILDREN_LOADER,
				DataLoaders.Loader.PARENT_LOADER,
				DataLoaders.Loader.FOR_VERSION_CONTENT_LOADER,
				DataLoaders.Loader.FOR_TYPE_CONTENT_LOADER,
				DataLoaders.Loader.BREADCRUMBS_LOADER,
				DataLoaders.Loader.MICRONODE_LOADER,
				DataLoaders.Loader.LIST_ITEM_LOADER);

		contentInterceptor.initializeDataLoaders((Collection<Node>) page.getWrappedList(), ac, dataLoaderToInitialize);

		// prepare permissions on nodes and tags
		Branch branch = Tx.get().getBranch(ac);
		PersistingUserDao userDao = CommonTx.get().userDao();
		List<Object> permIds = new ArrayList<>();
		permIds.addAll(page.getWrappedList().stream().map(CoreElement::getId).collect(Collectors.toList()));
		permIds.addAll(page.getWrappedList().stream().flatMap(item -> ((Node)item).getTags(branch).stream()).map(Tag::getId).collect(Collectors.toSet()));
		userDao.preparePermissionsForElementIds(ac.getUser(), permIds);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void beforeTransformToRestSync(Page<? extends CoreElement<? extends RestModel>> page,
			InternalActionContext ac) {
		ContentInterceptor contentInterceptor = HibernateTx.get().getContentInterceptor();
		List<DataLoaders.Loader> dataLoaderToInitialize = Arrays.asList(
				DataLoaders.Loader.CHILDREN_LOADER,
				DataLoaders.Loader.PARENT_LOADER,
				DataLoaders.Loader.FOR_VERSION_CONTENT_LOADER,
				DataLoaders.Loader.FOR_TYPE_CONTENT_LOADER,
				DataLoaders.Loader.BREADCRUMBS_LOADER,
				DataLoaders.Loader.MICRONODE_LOADER,
				DataLoaders.Loader.LIST_ITEM_LOADER);

		contentInterceptor.initializeDataLoaders((Collection<Node>) page.getWrappedList(), ac, dataLoaderToInitialize);

		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();
		if (fields.has("perms")) {
			PersistingUserDao userDao = CommonTx.get().userDao();
			List<Object> nodeIds = page.getWrappedList().stream().map(CoreElement::getId).collect(Collectors.toList());
			userDao.preparePermissionsForElementIds(ac.getUser(), nodeIds);
		}
	}

	/**
	 * Loads nodes for the given ids with their associated content edges
	 * @param nodeUuids
	 * @return
	 */
	public List<? extends Node> loadNodesWithEdges(Collection<UUID> nodeUuids) {
		EntityGraph<?> entityGraph = em().getEntityGraph("node.content");
		return SplittingUtils.splitAndMergeInList(nodeUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em().createNamedQuery("node.findNodesByUuids", HibNodeImpl.class)
				.setParameter("nodeUuids", slice)
				.setHint("jakarta.persistence.fetchgraph", entityGraph)
				.getResultList());
	}

	/**
	 * Loads nodes for the given ids with their associated content edges
	 * @param nodeUuids
	 * @return
	 */
	public List<? extends Node> loadNodesWithEdgesAndTags(List<UUID> nodeUuids) {
		EntityGraph<?> entityGraph = em().getEntityGraph("node.contentAndTags");
		return em().createNamedQuery("node.findNodesByUuids", HibNodeImpl.class)
				.setParameter("nodeUuids", nodeUuids)
				.setHint("jakarta.persistence.fetchgraph", entityGraph)
				.getResultList();
	}

	/**
	 * Delete all the nodes related to the project.
	 * @param project
	 * @param bac
	 */
	public void deleteAllFromProject(Project project, BulkActionContext bac) {
		nodeDeleteDao.deleteAllFromProject(project, bac);
	}

	@Override
	public void deleteFromBranch(Node node, InternalActionContext ac, Branch branch, BulkActionContext bac, boolean ignoreChecks) {
		DeleteParameters parameters = ac.getDeleteParameters();
		if (parameters.isRecursive()) {
			nodeDeleteDao.deleteRecursiveFromBranch(node, branch, bac, ignoreChecks);
		} else {
			PersistingNodeDao.super.deleteFromBranch(node, ac, branch, bac, ignoreChecks);
		}
	}
	@Override
	public void delete(Node node, BulkActionContext bac, boolean ignoreChecks, boolean recursive) {
		if (recursive) {
			nodeDeleteDao.deleteRecursive(node, bac, ignoreChecks);
		} else {
			PersistingNodeDao.super.delete(node, bac, ignoreChecks, recursive);
		}
	}

	private Map<UUID, NodeFieldContainer> getFieldContainers(Collection<Role> roles, Collection<Node> nodes, List<String> languageTags, String branchUuid, ContainerType type) {
		return getFieldsContainers(roles, nodes, type, branchUuid, languageTags);
	}

	private Map<UUID, NodeFieldContainer> getFieldsContainers(Collection<Role> roles, Collection<Node> nodes, ContainerType type, String branchUuid, List<String> languageTags) {
		List<HibNodeFieldContainerEdgeImpl> edges;
		if (roles.isEmpty()) {
			edges = SplittingUtils.splitAndMergeInList(nodes, HibernateUtil.inQueriesLimitForSplitting(3 + languageTags.size()), (params) -> {
				return em().createNamedQuery("contentEdge.findByNodeTypeBranchAndLanguageForAdmin", HibNodeFieldContainerEdgeImpl.class)
						.setParameter("nodes", params)
						.setParameter("type", type)
						.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
						.setParameter("languageTags", languageTags)
						.getResultList();
			});
		} else {
			edges = SplittingUtils.splitAndMergeInList(nodes, HibernateUtil.inQueriesLimitForSplitting(3 + roles.size() + languageTags.size()), (params) -> {
				return em().createNamedQuery("contentEdge.findByNodeTypeBranchAndLanguage", HibNodeFieldContainerEdgeImpl.class)
						.setParameter("nodes", params)
						.setParameter("type", type)
						.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
						.setParameter("languageTags", languageTags)
						.setParameter("roles", roles)
						.getResultList();
			});
		}

		List<HibNodeFieldContainerImpl> schemaContent = contentStorage.findMany(edges);

		return schemaContent.stream()
				.collect(collectingByNodeWithLanguageFallback(languageTags));
	}

	/**
	 * Collect a stream of HibNodeFieldContainerImpl to a Map<HibNode, HibNodeFieldContainerImpl>, using the languages provided
	 * If the languages are en,de,it and a node has a container for all of those languages, the english one will be used.
	 * It is responsibility of the caller to make sure that the stream contains only content that have one of the provided language tags
	 * @param languageTags
	 * @return
	 */
	private Collector<HibNodeFieldContainerImpl, ?, Map<UUID, NodeFieldContainer>> collectingByNodeWithLanguageFallback(List<String> languageTags)  {
		return Collectors.toMap(c -> c.get(CommonContentColumn.NODE, () -> null, Function.identity()), Function.identity(), (a, b) -> {
			int indexOfA = languageTags.indexOf(a.getLanguageTag());
			int indexOfB = languageTags.indexOf(b.getLanguageTag());

			return indexOfA < indexOfB ? a : b;
		});
	}

	public NativeJoin makeParentJoin(String nodeAlias, Set<Node> parents, Optional<UUID> maybeBranch) {
		String branchParentAlias = makeAlias(databaseConnector.maybeGetDatabaseEntityName(HibBranchNodeParent.class).get());
		NativeJoin mj = new NativeJoin();
		maybeBranch.ifPresent(branchUuid -> {
			String branchParentUuidParam = makeParamName(branchUuid);
			mj.appendWhereClause(String.format(" ( %s.%s = :%s ) ", branchParentAlias, databaseConnector.renderNonContentColumn("branchParent_dbUuid"), branchParentUuidParam));
			mj.getParameters().put(branchParentUuidParam, branchUuid);
		});
		Optional.of(parents != null && parents.size() > 0).filter(b -> b).ifPresent(yes -> {
			String parentNodesParam = makeParamName(parents);
			mj.appendWhereClause(String.format(" ( %s.%s IN :%s) ", branchParentAlias, databaseConnector.renderNonContentColumn("nodeParent_dbUuid"), parentNodesParam));
			mj.getParameters().put(parentNodesParam, parents.stream().map(Node::getId).collect(Collectors.toSet()));
		});
		{
			Integer distance = Integer.valueOf(1);
			String parentalDistanceParam = makeParamName(distance);
			mj.appendWhereClause(String.format(" ( %s.%s = :%s ) ", branchParentAlias, databaseConnector.renderNonContentColumn("distance"), parentalDistanceParam));
			mj.getParameters().put(parentalDistanceParam, distance);
		}
		mj.addJoin(databaseConnector.maybeGetPhysicalTableName(HibBranchNodeParent.class).get(), branchParentAlias, 
				new String[] {nodeAlias + "." + databaseConnector.renderNonContentColumn("dbUuid")}, new String[] {databaseConnector.renderNonContentColumn("child_dbUuid")},
				JoinType.INNER,
				nodeAlias);
		return mj;
	}

	private Optional<Function<Node, Node>> maybeParentLoader() {
		HibernateTx tx = HibernateTx.get();
		ContentInterceptor contentInterceptor = tx.getContentInterceptor();
		return contentInterceptor.getDataLoaders().flatMap(DataLoaders::getParentLoader);
	}

	private Optional<Function<Node, List<Node>>> maybeChildrenLoader() {
		HibernateTx tx = HibernateTx.get();
		ContentInterceptor contentInterceptor = tx.getContentInterceptor();
		return contentInterceptor.getDataLoaders().flatMap(DataLoaders::getChildrenLoader);
	}

	private Optional<Function<Node, List<Node>>> maybeBreadcrumbsLoader() {
		HibernateTx tx = HibernateTx.get();
		ContentInterceptor contentInterceptor = tx.getContentInterceptor();
		return contentInterceptor.getDataLoaders().flatMap(DataLoaders::getBreadcrumbsLoader);
	}

	@Override
	public Set<String> findUsedLanguages(Project project, Collection<String> languageTags, boolean assignedLanguagesOnly) {
		TypedQuery<String> query;
		if (assignedLanguagesOnly) {
			query = em().createNamedQuery("contentEdge.findUsedKnownLanguages", String.class);
		} else {
			query = em().createNamedQuery("contentEdge.findUsedLanguages", String.class);
		}
		return query
				.setParameter("project", project)
				.setParameter("tags", languageTags)
				.getResultStream().collect(Collectors.toSet());
	}
}
