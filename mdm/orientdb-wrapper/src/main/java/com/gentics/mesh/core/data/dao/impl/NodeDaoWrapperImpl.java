package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.List;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractRootDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.path.Path;

import dagger.Lazy;

/**
 * DAO for {@link HibNode} operation.
 */
@Singleton
public class NodeDaoWrapperImpl extends AbstractRootDaoWrapper<NodeResponse, HibNode, Node, HibProject> implements NodeDaoWrapper {

	@Inject
	public NodeDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
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
	public long computeCount(HibProject project) {
		return toGraph(project).getNodeRoot().computeCount();
	}

	@Override
	public HibNode create(HibNode parentNode, HibUser creator, HibSchemaVersion schemaVersion, HibProject project) {
		return toGraph(parentNode).create(creator, schemaVersion, project);
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
	public HibNode create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return toGraph(project).getNodeRoot().create(ac, batch, uuid);
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
	public Page<? extends HibNode> getChildren(HibNode node, InternalActionContext ac, List<String> languageTags, String branchUuid,
		ContainerType type, PagingParameters pagingParameter) {
		return toGraph(node).getChildren(ac, languageTags, branchUuid, type, pagingParameter);
	}

	@Override
	public List<String> getAvailableLanguageNames(HibNode node) {
		return toGraph(node).getAvailableLanguageNames();
	}

	@Override
	public String getDisplayName(HibNode node, InternalActionContext ac) {
		return toGraph(node).getDisplayName(ac);
	}

	@Override
	public void moveTo(HibNode sourceNode, InternalActionContext ac, HibNode targetNode, EventQueueBatch batch) {
		toGraph(sourceNode).moveTo(ac, toGraph(targetNode), batch);
	}

	@Override
	public NavigationResponse transformToNavigation(HibNode node, InternalActionContext ac) {
		return toGraph(node).transformToNavigation(ac);
	}

	@Override
	public PublishStatusResponse transformToPublishStatus(HibNode node, InternalActionContext ac) {
		return toGraph(node).transformToPublishStatus(ac);
	}

	@Override
	public void publish(HibNode node, InternalActionContext ac, BulkActionContext bac) {
		toGraph(node).publish(ac, bac);
	}

	@Override
	public void takeOffline(HibNode node, InternalActionContext ac, BulkActionContext bac) {
		toGraph(node).takeOffline(ac, bac);
	}

	@Override
	public PublishStatusModel transformToPublishStatus(HibNode node, InternalActionContext ac, String languageTag) {
		return toGraph(node).transformToPublishStatus(ac, languageTag);
	}

	@Override
	public void publish(HibNode node, InternalActionContext ac, BulkActionContext bac, String languageTag) {
		toGraph(node).publish(ac, bac, languageTag);
	}

	@Override
	public void setPublished(HibNode node, InternalActionContext ac, HibNodeFieldContainer container, String branchUuid) {
		toGraph(node).setPublished(ac, container, branchUuid);
	}

	@Override
	public void takeOffline(HibNode node, InternalActionContext ac, BulkActionContext bac, HibBranch branch, String languageTag) {
		toGraph(node).takeOffline(ac, bac, branch, languageTag);
	}

	@Override
	public String getPath(HibNode node, ActionContext ac, String branchUuid, ContainerType type, String... languageTag) {
		return toGraph(node).getPath(ac, branchUuid, type, languageTag);
	}

	@Override
	public Path resolvePath(HibNode baseNode, String branchUuid, ContainerType type, Path nodePath, Stack<String> pathStack) {
		return toGraph(baseNode).resolvePath(branchUuid, type, nodePath, pathStack);
	}

	@Override
	public void delete(HibNode node, BulkActionContext bac, boolean ignoreChecks, boolean recursive) {
		toGraph(node).delete(bac, ignoreChecks, recursive);
	}

	@Override
	public Result<? extends HibNode> getBreadcrumbNodes(HibNode node, InternalActionContext ac) {
		return toGraph(node).getBreadcrumbNodes(ac);
	}

	@Override
	public boolean isBaseNode(HibNode node) {
		return toGraph(node).isBaseNode();
	}

	@Override
	public boolean isVisibleInBranch(HibNode node, String branchUuid) {
		return toGraph(node).isVisibleInBranch(branchUuid);
	}

	@Override
	public NodeVersionsResponse transformToVersionList(HibNode node, InternalActionContext ac) {
		return toGraph(node).transformToVersionList(ac);
	}

	@Override
	public boolean update(HibNode node, InternalActionContext ac, EventQueueBatch batch) {
		return toGraph(node).update(ac, batch);
		// return ac.getProject().getNodeRoot().update(element, ac, batch);
	}

	@Override
	public String getAPIPath(HibNode node, InternalActionContext ac) {
		return toGraph(node).getAPIPath(ac);
	}

	@Override
	public String getETag(HibNode node, InternalActionContext ac) {
		return toGraph(node).getETag(ac);
	}

	@Override
	public Page<? extends HibTag> updateTags(HibNode node, InternalActionContext ac, EventQueueBatch batch) {
		Node graphNode = toGraph(node);
		return graphNode.updateTags(ac, batch);
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
	public void delete(HibProject root, HibNode element, BulkActionContext bac) {
		toGraph(root).getNodeRoot().delete(toGraph(element), bac);
	}

	@Override
	public boolean update(HibProject root, HibNode element, InternalActionContext ac, EventQueueBatch batch) {
		return toGraph(root).getNodeRoot().update(toGraph(element), ac, batch);
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
}
