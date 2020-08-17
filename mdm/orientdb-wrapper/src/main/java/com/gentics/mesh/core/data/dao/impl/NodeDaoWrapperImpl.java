package com.gentics.mesh.core.data.dao.impl;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

@Singleton
public class NodeDaoWrapperImpl implements NodeDaoWrapper {
	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public NodeDaoWrapperImpl(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
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
	public NodeResponse transformToRestSync(Node element, InternalActionContext ac, int level, String... languageTags) {
		return element.transformToRestSync(ac, level, languageTags);
	}


	@Override
	public Node create(Node parentNode, HibUser creator, SchemaVersion schemaVersion, HibProject project) {
		return parentNode.create(creator, schemaVersion, project);
	}

	@Override
	public Node create(Node parentNode, HibUser creator, SchemaVersion schemaVersion, HibProject project, HibBranch branch, String uuid) {
		return parentNode.create(creator, schemaVersion, project, branch, uuid);
	}

	@Override
	public TraversalResult<Node> getChildren(Node node) {
		return node.getChildren();
	}

	@Override
	public TraversalResult<Node> getChildren(Node node, String branchUuid) {
		return node.getChildren(branchUuid);
	}

	@Override
	public Stream<Node> getChildrenStream(Node node, InternalActionContext ac) {
		return node.getChildrenStream(ac);
	}

	@Override
	public Node getParentNode(Node node, String branchUuid) {
		return node.getParentNode(branchUuid);
	}

	@Override
	public void setParentNode(Node node, String branchUuid, Node parentNode) {
		node.setParentNode(branchUuid, parentNode);
	}

	@Override
	public TransformablePage<Node> getChildren(Node node, InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type, PagingParameters pagingParameter) {
		return node.getChildren(ac, languageTags, branchUuid, type, pagingParameter);
	}

	@Override
	public List<String> getAvailableLanguageNames(Node node) {
		return node.getAvailableLanguageNames();
	}

	@Override
	public String getDisplayName(Node node, InternalActionContext ac) {
		return node.getDisplayName(ac);
	}

	@Override
	public void moveTo(Node sourceNode, InternalActionContext ac, Node targetNode, EventQueueBatch batch) {
		sourceNode.moveTo(ac, targetNode, batch);
	}

	@Override
	public NavigationResponse transformToNavigation(Node node, InternalActionContext ac) {
		return node.transformToNavigation(ac);
	}

	@Override
	public PublishStatusResponse transformToPublishStatus(Node node, InternalActionContext ac) {
		return node.transformToPublishStatus(ac);
	}

	@Override
	public void publish(Node node, InternalActionContext ac, BulkActionContext bac) {
		node.publish(ac, bac);
	}

	@Override
	public void takeOffline(Node node, InternalActionContext ac, BulkActionContext bac) {
		node.takeOffline(ac, bac);
	}

	@Override
	public PublishStatusModel transformToPublishStatus(Node node, InternalActionContext ac, String languageTag) {
		return node.transformToPublishStatus(ac, languageTag);
	}

	@Override
	public void publish(Node node, InternalActionContext ac, BulkActionContext bac, String languageTag) {
		node.publish(ac, bac, languageTag);
	}

	@Override
	public void setPublished(Node node, InternalActionContext ac, NodeGraphFieldContainer container, String branchUuid) {
		node.setPublished(ac, container, branchUuid);
	}

	@Override
	public void takeOffline(Node node, InternalActionContext ac, BulkActionContext bac, HibBranch branch, String languageTag) {
		node.takeOffline(ac, bac, branch, languageTag);
	}
}
