package com.gentics.mesh.core.data.dao.impl;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.madl.traversal.TraversalResult;

public class ContentDaoWrapperImpl implements ContentDaoWrapper {

	@Inject
	public ContentDaoWrapperImpl() {
	}

	@Override
	public NodeGraphFieldContainer getLatestDraftFieldContainer(Node node, String languageTag) {
		return node.getLatestDraftFieldContainer(languageTag);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(Node node, String languageTag, HibBranch branch, ContainerType type) {
		return node.getGraphFieldContainer(languageTag, branch, type);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(Node node, String languageTag) {
		return node.getGraphFieldContainer(languageTag);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(Node node, String languageTag, String branchUuid, ContainerType type) {
		return node.getGraphFieldContainer(languageTag, branchUuid, type);
	}

	@Override
	public NodeGraphFieldContainer createGraphFieldContainer(Node node, String languageTag, HibBranch branch, HibUser user) {
		return node.createGraphFieldContainer(languageTag, branch, user);
	}

	@Override
	public NodeGraphFieldContainer createGraphFieldContainer(Node node, String languageTag, HibBranch branch, HibUser editor, NodeGraphFieldContainer original, boolean handleDraftEdge) {
		return node.createGraphFieldContainer(languageTag, branch, editor, original, handleDraftEdge);
	}

	@Override
	public TraversalResult<NodeGraphFieldContainer> getDraftGraphFieldContainers(Node node) {
		return node.getDraftGraphFieldContainers();
	}

	@Override
	public TraversalResult<NodeGraphFieldContainer> getGraphFieldContainers(Node node, String branchUuid, ContainerType type) {
		return node.getGraphFieldContainers(branchUuid, type);
	}

	@Override
	public TraversalResult<NodeGraphFieldContainer> getGraphFieldContainers(Node node, ContainerType type) {
		return node.getGraphFieldContainers(type);
	}

	@Override
	public long getGraphFieldContainerCount(Node node) {
		return node.getGraphFieldContainerCount();
	}

	@Override
	public NodeGraphFieldContainer findVersion(Node node, List<String> languageTags, String branchUuid, String version) {
		return node.findVersion(languageTags, branchUuid, version);
	}

	@Override
	public void deleteLanguageContainer(Node node, InternalActionContext ac, HibBranch branch, String languageTag, BulkActionContext bac, boolean failForLastContainer) {
		node.deleteLanguageContainer(ac, branch, languageTag, bac, failForLastContainer);
	}

	@Override
	public String getPathSegment(Node node, String branchUuid, ContainerType type, boolean anyLanguage, String... languageTag) {
		return node.getPathSegment(branchUuid, type, anyLanguage, languageTag);
	}

	@Override
	public void deleteFromBranch(Node node, InternalActionContext ac, HibBranch branch, BulkActionContext bac, boolean ignoreChecks) {
		node.deleteFromBranch(ac, branch, bac, ignoreChecks);
	}

	@Override
	public NodeGraphFieldContainer publish(Node node, InternalActionContext ac, String languageTag, HibBranch branch, HibUser user) {
		return node.publish(ac, languageTag, branch, user);
	}

	@Override
	public Stream<NodeGraphField> getInboundReferences(Node node) {
		return node.getInboundReferences();
	}
}
