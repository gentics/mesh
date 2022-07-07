package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.branch.HibBranch;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.dao.AbstractRootDaoWrapper;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

/**
 * DAO for {@link HibNode} operation.
 */
@Singleton
public class NodeDaoWrapperImpl extends AbstractRootDaoWrapper<NodeResponse, HibNode, Node, HibProject> implements NodeDaoWrapper {

	@Inject
	public NodeDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot) {
		super(boot);
	}

	@Override
	public HibNode findByUuid(HibProject project, String uuid) {
		return toGraph(project).getNodeRoot().findByUuid(uuid);
	}

	@Override
	public HibNode findByName(HibProject project, String name) {
		return toGraph(project).getNodeRoot().findByName(name);
	}

	@Override
	public Result<? extends HibNode> findAll(HibProject project) {
		return toGraph(project).findNodes();
		// return toGraph(project).getNodeRoot().findAll();
	}

	@Override
	public Page<? extends HibNode> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo) {
		return toGraph(project).getNodeRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibNode> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<HibNode> extraFilter) {
		return toGraph(project).getNodeRoot().findAll(ac, pagingInfo, node -> {
			return extraFilter.test(node);
		});
	}

	@Override
	public long count(HibProject project) {
		return toGraph(project).getNodeRoot().computeCount();
	}

	@Override
	public Result<? extends HibNode> getChildren(HibNode node) {
		return toGraph(node).getChildren();
	}

	@Override
	public Result<? extends HibNode> getChildren(HibNode node, String branchUuid) {
		return toGraph(node).getChildren(branchUuid);
	}

	@Override
	public Stream<? extends HibNode> getChildrenStream(HibNode node, InternalActionContext ac, InternalPermission perm) {
		return toGraph(node).getChildrenStream(ac, perm);
	}

	@Override
	public Map<HibNode, List<HibNode>> getChildren(Collection<HibNode> nodes, String branchUuid) {
		return nodes.stream()
				.map(node -> Pair.of(node, (List<HibNode>) getChildren(node, branchUuid).list()))
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	@Override
	public HibNode getParentNode(HibNode node, String branchUuid) {
		return toGraph(node).getParentNode(branchUuid);
	}

	@Override
	public Map<HibNode, HibNode> getParentNodes(Collection<HibNode> nodes, String branchUuid) {
		return nodes.stream()
				.map(node -> Pair.of(node, getParentNode(node, branchUuid)))
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	@Override
	public void setParentNode(HibNode node, String branchUuid, HibNode parentNode) {
		toGraph(node).setParentNode(branchUuid, toGraph(parentNode));
	}

	@Override
	public void removeParent(HibNode node, String branchUuid) {
		toGraph(node).removeParent(branchUuid);
	}

	@Override
	public Page<? extends HibNode> getChildren(HibNode node, InternalActionContext ac, List<String> languageTags, String branchUuid,
		ContainerType type, PagingParameters pagingParameter) {
		return toGraph(node).getChildren(ac, languageTags, branchUuid, type, pagingParameter);
	}

	@Override
	public Map<HibNode, List<NodeContent>> getChildren(Set<HibNode> nodes, InternalActionContext ac, String branchUuid, List<String> languageTags, ContainerType type) {
		HibUser user = ac.getUser();
		return nodes.stream()
				.map(node -> Pair.of(node, getContentFromNode(Tx.get(), user, node, branchUuid, languageTags, type)))
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	private List<NodeContent> getContentFromNode(Tx tx, HibUser user, HibNode sourceNode, String branchUuid, List<String> languageTags, ContainerType type) {
		UserDao userDao = tx.userDao();
		ContentDao contentDao = tx.contentDao();
		return toGraph(sourceNode).getChildren(branchUuid)
				.stream()
				.filter(node -> userDao.hasPermissionForId(user, node.getId(), type == PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM))
				.map(node -> {
					HibNodeFieldContainer container = contentDao.findVersion(node, languageTags, branchUuid, type.getHumanCode());
					return new NodeContent(node, container, languageTags, type);
				})
				.collect(Collectors.toList());
	}

	@Override
	public void removeElement(HibNode node) {
		toGraph(node).removeElement();
	}

	@Override
	public boolean isBaseNode(HibNode node) {
		return toGraph(node).isBaseNode();
	}

	@Override
	public Stream<? extends HibNode> findAllStream(HibProject project, InternalActionContext ac, InternalPermission perm) {
		Project graphProject = toGraph(project);
		return graphProject.getNodeRoot().findAllStream(ac, perm);
	}

	@Override
	public Page<? extends HibNode> findAllNoPerm(HibProject root, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return toGraph(root).getNodeRoot().findAllNoPerm(ac, pagingInfo);
	}

	@Override
	public void addItem(HibProject root, HibNode item) {
		toGraph(root).getNodeRoot().addItem(toGraph(item));
	}

	@Override
	public void removeItem(HibProject root, HibNode item) {
		toGraph(root).getNodeRoot().removeItem(toGraph(item));
	}

	@Override
	public Class<? extends HibNode> getPersistenceClass(HibProject root) {
		return toGraph(root).getNodeRoot().getPersistanceClass();
	}

	@Override
	public long globalCount(HibProject root) {
		return toGraph(root).getNodeRoot().globalCount();
	}

	@Override
	public HibNode findByUuidGlobal(String uuid) {
		return boot.get().meshRoot().findNodeByUuid(uuid);
	}

	@Override
	public long globalCount() {
		return boot.get().meshRoot().nodeCount();
	}

	@Override
	public Map<HibNode, List<HibNode>> getBreadcrumbNodesMap(Collection<HibNode> nodes, InternalActionContext ac) {
		return nodes.stream().map(node -> Pair.of(node,
				getBreadcrumbNodes(node, ac)
						.stream()
						.map(HibNode.class::cast)
						.collect(Collectors.toList()))
		)
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	@Override
	protected RootVertex<Node> getRoot(HibProject root) {
		return toGraph(root).getNodeRoot();
	}

	@Override
	public Stream<? extends HibNode> findAllGlobal() {
		return boot.get().meshRoot().findAllNodes().stream();
	}

	@Override
	public Iterator<? extends HibNodeFieldContainerEdge> getWebrootEdges(HibNode node, String segmentInfo, String branchUuid, ContainerType type) {
		return toGraph(node).getWebrootEdges(segmentInfo, branchUuid, type);
	}

	@Override
	public Stream<HibNodeField> getInboundReferences(HibNode node) {
		return toGraph(node).getInboundReferences();
	}

	@Override
	public HibNode createPersisted(HibProject root, String uuid) {
		HibNode node = toGraph(root).getNodeRoot().create();
		if (uuid != null) {
			node.setUuid(uuid);
		}
		node.setProject(root);
		return node;
	}

	@Override
	public void migrateParentNodes(List<? extends HibNode> nodes, HibBranch oldBranch, HibBranch newBranch) {
		nodes.forEach(node -> {
			HibNode oldParent = getParentNode(node, oldBranch.getUuid());
			if (oldParent != null) {
				setParentNode(node, newBranch.getUuid(), oldParent);
			}
		});
	}
}
