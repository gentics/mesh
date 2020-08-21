package com.gentics.mesh.core.data.dao.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.util.VersionNumber;

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

	@Override
	public void delete(NodeGraphFieldContainer content, BulkActionContext bac) {
		content.delete(bac);
	}

	@Override
	public void delete(NodeGraphFieldContainer content, BulkActionContext bac, boolean deleteNext) {
		content.delete(bac, deleteNext);
	}

	@Override
	public void deleteFromBranch(NodeGraphFieldContainer content, HibBranch branch, BulkActionContext bac) {
		content.deleteFromBranch(branch, bac);
	}

	@Override
	public String getDisplayFieldValue(NodeGraphFieldContainer content) {
		return content.getDisplayFieldValue();
	}

	@Override
	public Node getNode(NodeGraphFieldContainer content) {
		return content.getNode();
	}

	@Override
	public void updateWebrootPathInfo(NodeGraphFieldContainer content, InternalActionContext ac, String branchUuid, String conflictI18n) {
		content.updateWebrootPathInfo(ac, branchUuid, conflictI18n);
	}

	@Override
	public VersionNumber getVersion(NodeGraphFieldContainer content) {
		return content.getVersion();
	}

	@Override
	public void setVersion(NodeGraphFieldContainer content, VersionNumber version) {
		content.setVersion(version);
	}

	@Override
	public boolean hasNextVersion(NodeGraphFieldContainer content) {
		return content.hasNextVersion();
	}

	@Override
	public Iterable<NodeGraphFieldContainer> getNextVersions(NodeGraphFieldContainer content) {
		return content.getNextVersions();
	}

	@Override
	public void setNextVersion(NodeGraphFieldContainer current, NodeGraphFieldContainer next) {
		current.setNextVersion(next);
	}

	@Override
	public boolean hasPreviousVersion(NodeGraphFieldContainer content) {
		return content.hasPreviousVersion();
	}

	@Override
	public NodeGraphFieldContainer getPreviousVersion(NodeGraphFieldContainer content) {
		return content.getPreviousVersion();
	}

	@Override
	public void clone(NodeGraphFieldContainer dest, NodeGraphFieldContainer src) {
		dest.clone(src);
	}

	@Override
	public boolean isType(NodeGraphFieldContainer content, ContainerType type) {
		return content.isType(type);
	}

	@Override
	public boolean isType(NodeGraphFieldContainer content, ContainerType type, String branchUuid) {
		return content.isType(type, branchUuid);
	}

	@Override
	public Set<String> getBranches(NodeGraphFieldContainer content, ContainerType type) {
		return content.getBranches(type);
	}

	@Override
	public List<FieldContainerChange> compareTo(NodeGraphFieldContainer content, NodeGraphFieldContainer container) {
		return content.compareTo(container);
	}

	@Override
	public List<FieldContainerChange> compareTo(NodeGraphFieldContainer content, FieldMap fieldMap) {
		return content.compareTo(fieldMap);
	}

	@Override
	public SchemaVersion getSchemaContainerVersion(NodeGraphFieldContainer content) {
		return content.getSchemaContainerVersion();
	}

	@Override
	public List<MicronodeGraphField> getMicronodeFields(NodeGraphFieldContainer content, MicroschemaVersion version) {
		return content.getMicronodeFields(version);
	}

	@Override
	public TraversalResult<MicronodeGraphFieldList> getMicronodeListFields(NodeGraphFieldContainer content, MicroschemaVersion version) {
		return content.getMicronodeListFields(version);
	}

	@Override
	public String getETag(NodeGraphFieldContainer content, InternalActionContext ac) {
		return content.getETag(ac);
	}

	@Override
	public void updateDisplayFieldValue(NodeGraphFieldContainer content) {
		content.updateDisplayFieldValue();
	}

	@Override
	public String getSegmentFieldValue(NodeGraphFieldContainer content) {
		return content.getSegmentFieldValue();
	}

	@Override
	public void postfixSegmentFieldValue(NodeGraphFieldContainer content) {
		content.postfixSegmentFieldValue();
	}

	@Override
	public Set<String> getUrlFieldValues(NodeGraphFieldContainer content) {
		return content.getUrlFieldValues();
	}

	@Override
	public Path getPath(NodeGraphFieldContainer content, InternalActionContext ac) {
		return content.getPath(ac);
	}

	@Override
	public Iterator<GraphFieldContainerEdge> getContainerEdge(NodeGraphFieldContainer content, ContainerType type, String branchUuid) {
		return content.getContainerEdge(type, branchUuid);
	}

	@Override
	public NodeMeshEventModel onDeleted(NodeGraphFieldContainer content, String branchUuid, ContainerType type) {
		return content.onDeleted(branchUuid, type);
	}

	@Override
	public NodeMeshEventModel onCreated(NodeGraphFieldContainer content, String branchUuid, ContainerType type) {
		return content.onCreated(branchUuid, type);
	}

	@Override
	public NodeMeshEventModel onUpdated(NodeGraphFieldContainer content, String branchUuid, ContainerType type) {
		return content.onUpdated(branchUuid, type);
	}

	@Override
	public NodeMeshEventModel onTakenOffline(NodeGraphFieldContainer content, String branchUuid) {
		return content.onTakenOffline(branchUuid);
	}

	@Override
	public NodeMeshEventModel onPublish(NodeGraphFieldContainer content, String branchUuid) {
		return content.onPublish(branchUuid);
	}

	@Override
	public VersionInfo transformToVersionInfo(NodeGraphFieldContainer content, InternalActionContext ac) {
		return content.transformToVersionInfo(ac);
	}

	@Override
	public boolean isPurgeable(NodeGraphFieldContainer content) {
		return content.isPurgeable();
	}

	@Override
	public boolean isAutoPurgeEnabled(NodeGraphFieldContainer content) {
		return content.isAutoPurgeEnabled();
	}

	@Override
	public void purge(NodeGraphFieldContainer content, BulkActionContext bac) {
		content.purge(bac);
	}

	@Override
	public TraversalResult<NodeGraphFieldContainer> versions(NodeGraphFieldContainer content) {
		return content.versions();
	}

	@Override
	public String getLanguageTag(NodeGraphFieldContainer content) {
		return content.getLanguageTag();
	}

	@Override
	public void setLanguageTag(NodeGraphFieldContainer content, String languageTag) {
		content.setLanguageTag(languageTag);
	}
}
