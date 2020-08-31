package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toNode;
import static com.gentics.mesh.core.data.util.HibClassConverter.toProject;

import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
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

@Singleton
public class NodeDaoWrapperImpl extends AbstractDaoWrapper<HibNode> implements NodeDaoWrapper {

	@Inject
	public NodeDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions) {
		super(boot, permissions);
	}

	@Override
	public HibNode loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm) {
		return toProject(project).getNodeRoot().loadObjectByUuid(ac, uuid, perm);
	}

	@Override
	public Node findByUuid(HibProject project, String uuid) {
		return project.getNodeRoot().findByUuid(uuid);
	}

	@Override
	public Node findByUuidGlobal(String uuid) {
		// TODO Probably wrong interface hierarchy. There is no need for this method
		throw new RuntimeException("Not implemented");
	}

	@Override
	public long computeGlobalCount() {
		// TODO Probably wrong interface hierarchy. There is no need for this method
		throw new RuntimeException("Not implemented");
	}

	@Override
	public NodeResponse transformToRestSync(HibNode node, InternalActionContext ac, int level, String... languageTags) {
		return toNode(node).transformToRestSync(ac, level, languageTags);
	}

	@Override
	public HibNode create(HibNode parentNode, HibUser creator, HibSchemaVersion schemaVersion, HibProject project) {
		return toNode(parentNode).create(creator, schemaVersion, project);
	}

	@Override
	public HibNode create(HibNode parentNode, HibUser creator, HibSchemaVersion schemaVersion, HibProject project, HibBranch branch, String uuid) {
		return toNode(parentNode).create(creator, schemaVersion, project, branch, uuid);
	}

	@Override
	public Result<? extends HibNode> getChildren(HibNode node) {
		return toNode(node).getChildren();
	}

	@Override
	public Result<? extends HibNode> getChildren(HibNode node, String branchUuid) {
		return toNode(node).getChildren(branchUuid);
	}

	@Override
	public Stream<? extends HibNode> getChildrenStream(HibNode node, InternalActionContext ac) {
		return toNode(node).getChildrenStream(ac);
	}

	@Override
	public HibNode getParentNode(HibNode node, String branchUuid) {
		return toNode(node).getParentNode(branchUuid);
	}

	@Override
	public void setParentNode(HibNode node, String branchUuid, HibNode parentNode) {
		toNode(node).setParentNode(branchUuid, toNode(parentNode));
	}

	@Override
	public TransformablePage<? extends HibNode> getChildren(HibNode node, InternalActionContext ac, List<String> languageTags, String branchUuid,
		ContainerType type, PagingParameters pagingParameter) {
		return toNode(node).getChildren(ac, languageTags, branchUuid, type, pagingParameter);
	}

	@Override
	public List<String> getAvailableLanguageNames(HibNode node) {
		return toNode(node).getAvailableLanguageNames();
	}

	@Override
	public String getDisplayName(HibNode node, InternalActionContext ac) {
		return toNode(node).getDisplayName(ac);
	}

	@Override
	public void moveTo(HibNode sourceNode, InternalActionContext ac, HibNode targetNode, EventQueueBatch batch) {
		toNode(sourceNode).moveTo(ac, toNode(targetNode), batch);
	}

	@Override
	public NavigationResponse transformToNavigation(HibNode node, InternalActionContext ac) {
		return toNode(node).transformToNavigation(ac);
	}

	@Override
	public PublishStatusResponse transformToPublishStatus(HibNode node, InternalActionContext ac) {
		return toNode(node).transformToPublishStatus(ac);
	}

	@Override
	public void publish(HibNode node, InternalActionContext ac, BulkActionContext bac) {
		toNode(node).publish(ac, bac);
	}

	@Override
	public void takeOffline(HibNode node, InternalActionContext ac, BulkActionContext bac) {
		toNode(node).takeOffline(ac, bac);
	}

	@Override
	public PublishStatusModel transformToPublishStatus(HibNode node, InternalActionContext ac, String languageTag) {
		return toNode(node).transformToPublishStatus(ac, languageTag);
	}

	@Override
	public void publish(HibNode node, InternalActionContext ac, BulkActionContext bac, String languageTag) {
		toNode(node).publish(ac, bac, languageTag);
	}

	@Override
	public void setPublished(HibNode node, InternalActionContext ac, NodeGraphFieldContainer container, String branchUuid) {
		toNode(node).setPublished(ac, container, branchUuid);
	}

	@Override
	public void takeOffline(HibNode node, InternalActionContext ac, BulkActionContext bac, HibBranch branch, String languageTag) {
		toNode(node).takeOffline(ac, bac, branch, languageTag);
	}

	@Override
	public String getPath(HibNode node, ActionContext ac, String branchUuid, ContainerType type, String... languageTag) {
		return toNode(node).getPath(ac, branchUuid, type, languageTag);
	}

	@Override
	public Path resolvePath(HibNode baseNode, String branchUuid, ContainerType type, Path nodePath, Stack<String> pathStack) {
		return toNode(baseNode).resolvePath(branchUuid, type, nodePath, pathStack);
	}

	@Override
	public void delete(HibNode node, BulkActionContext bac, boolean ignoreChecks, boolean recursive) {
		toNode(node).delete(bac, ignoreChecks, recursive);
	}

	@Override
	public Result<? extends HibNode> getBreadcrumbNodes(HibNode node, InternalActionContext ac) {
		return toNode(node).getBreadcrumbNodes(ac);
	}

	@Override
	public boolean isBaseNode(HibNode node) {
		return toNode(node).isBaseNode();
	}

	@Override
	public boolean isVisibleInBranch(HibNode node, String branchUuid) {
		return toNode(node).isVisibleInBranch(branchUuid);
	}

	@Override
	public NodeVersionsResponse transformToVersionList(HibNode node, InternalActionContext ac) {
		return toNode(node).transformToVersionList(ac);
	}

	@Override
	public boolean update(HibNode node, InternalActionContext ac, EventQueueBatch batch) {
		return toNode(node).update(ac, batch);
		// return ac.getProject().getNodeRoot().update(element, ac, batch);
	}

	@Override
	public String getAPIPath(HibNode node, InternalActionContext ac) {
		return toNode(node).getAPIPath(ac);
	}

	@Override
	public String getETag(HibNode node, InternalActionContext ac) {
		return toNode(node).getETag(ac);
	}

}
