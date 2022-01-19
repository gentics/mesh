package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractRootDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
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
	public HibNode create(HibProject project, HibUser user, HibSchemaVersion version) {
		return toGraph(project).getNodeRoot().create(user, version, project);
	}

	@Override
	public HibNode create(HibNode parentNode, HibUser creator, HibSchemaVersion schemaVersion, HibProject project, HibBranch branch, String uuid) {
		return toGraph(parentNode).create(creator, schemaVersion, project, branch, uuid);
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
	public Stream<? extends HibNode> getChildrenStream(HibNode node, InternalActionContext ac) {
		return toGraph(node).getChildrenStream(ac);
	}

	@Override
	public HibNode getParentNode(HibNode node, String branchUuid) {
		return toGraph(node).getParentNode(branchUuid);
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
	public String getDisplayName(HibNode node, InternalActionContext ac) {
		return toGraph(node).getDisplayName(ac);
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
	public String getRootLabel(HibProject root) {
		return toGraph(root).getNodeRoot().getRootLabel();
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
}
