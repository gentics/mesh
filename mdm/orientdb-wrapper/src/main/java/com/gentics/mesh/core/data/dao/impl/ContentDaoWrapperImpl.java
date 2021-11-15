package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.util.VersionNumber;

public class ContentDaoWrapperImpl implements ContentDaoWrapper {

	private final Database db;

	@Inject
	public ContentDaoWrapperImpl(Database db) {
		this.db = db;
	}

	@Override
	public HibNodeFieldContainer getLatestDraftFieldContainer(HibNode node, String languageTag) {
		return toGraph(node).getLatestDraftFieldContainer(languageTag);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag, HibBranch branch, ContainerType type) {
		return toGraph(node).getFieldContainer(languageTag, branch, type);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag) {
		return toGraph(node).getFieldContainer(languageTag);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag, String branchUuid, ContainerType type) {
		return toGraph(node).getFieldContainer(languageTag, branchUuid, type);
	}

	@Override
	public HibNodeFieldContainer createFieldContainer(HibNode node, String languageTag, HibBranch branch, HibUser user) {
		return toGraph(node).createFieldContainer(languageTag, branch, user);
	}

	@Override
	public HibNodeFieldContainer createFieldContainer(HibNode node, String languageTag, HibBranch branch, HibUser editor,
			HibNodeFieldContainer original, boolean handleDraftEdge) {
		return toGraph(node).createFieldContainer(languageTag, branch, editor, original, handleDraftEdge);
	}

	@Override
	public Result<HibNodeFieldContainer> getDraftFieldContainers(HibNode node) {
		return toGraph(node).getDraftFieldContainers();
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(HibNode node, String branchUuid, ContainerType type) {
		return toGraph(node).getFieldContainers(branchUuid, type);
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(HibNode node, ContainerType type) {
		return toGraph(node).getFieldContainers(type);
	}

	@Override
	public long getFieldContainerCount(HibNode node) {
		return toGraph(node).getFieldContainerCount();
	}

	@Override
	public HibNodeFieldContainer findVersion(HibNode node, List<String> languageTags, String branchUuid, String version) {
		return toGraph(node).findVersion(languageTags, branchUuid, version);
	}

	@Override
	public void deleteLanguageContainer(HibNode node, InternalActionContext ac, HibBranch branch, String languageTag, BulkActionContext bac,
		boolean failForLastContainer) {
		toGraph(node).deleteLanguageContainer(ac, branch, languageTag, bac, failForLastContainer);
	}

	@Override
	public String getPathSegment(HibNode node, String branchUuid, ContainerType type, boolean anyLanguage, String... languageTag) {
		return toGraph(node).getPathSegment(branchUuid, type, anyLanguage, languageTag);
	}

	@Override
	public void deleteFromBranch(HibNode node, InternalActionContext ac, HibBranch branch, BulkActionContext bac, boolean ignoreChecks) {
		toGraph(node).deleteFromBranch(ac, branch, bac, ignoreChecks);
	}

	@Override
	public HibNodeFieldContainer publish(HibNode node, InternalActionContext ac, String languageTag, HibBranch branch, HibUser user) {
		return Tx.get().nodeDao().publish(node, ac, languageTag, branch, user);
	}

	@Override
	public Stream<HibNodeField> getInboundReferences(HibNode node) {
		return toGraph(node).getInboundReferences();
	}

	@Override
	public void delete(HibNodeFieldContainer content, BulkActionContext bac) {
		content.delete(bac);
	}

	@Override
	public void delete(HibNodeFieldContainer content, BulkActionContext bac, boolean deleteNext) {
		content.delete(bac, deleteNext);
	}

	@Override
	public void deleteFromBranch(HibNodeFieldContainer content, HibBranch branch, BulkActionContext bac) {
		content.deleteFromBranch(branch, bac);
	}

	@Override
	public String getDisplayFieldValue(HibNodeFieldContainer content) {
		return content.getDisplayFieldValue();
	}

	@Override
	public HibNode getNode(HibNodeFieldContainer content) {
		return content.getNode();
	}

	@Override
	public void updateWebrootPathInfo(HibNodeFieldContainer content, InternalActionContext ac, String branchUuid, String conflictI18n) {
		content.updateWebrootPathInfo(ac, branchUuid, conflictI18n);
	}

	@Override
	public VersionNumber getVersion(HibNodeFieldContainer content) {
		return content.getVersion();
	}

	@Override
	public void setVersion(HibNodeFieldContainer content, VersionNumber version) {
		content.setVersion(version);
	}

	@Override
	public boolean hasNextVersion(HibNodeFieldContainer content) {
		return content.hasNextVersion();
	}

	@Override
	public Iterable<HibNodeFieldContainer> getNextVersions(HibNodeFieldContainer content) {
		return content.getNextVersions();
	}

	@Override
	public void setNextVersion(HibNodeFieldContainer current, HibNodeFieldContainer next) {
		current.setNextVersion(next);
	}

	@Override
	public boolean hasPreviousVersion(HibNodeFieldContainer content) {
		return content.hasPreviousVersion();
	}

	@Override
	public HibNodeFieldContainer getPreviousVersion(HibNodeFieldContainer content) {
		return content.getPreviousVersion();
	}

	@Override
	public void clone(HibNodeFieldContainer dest, HibNodeFieldContainer src) {
		dest.clone(src);
	}

	@Override
	public boolean isType(HibNodeFieldContainer content, ContainerType type) {
		return content.isType(type);
	}

	@Override
	public boolean isType(HibNodeFieldContainer content, ContainerType type, String branchUuid) {
		return content.isType(type, branchUuid);
	}

	@Override
	public Set<String> getBranches(HibNodeFieldContainer content, ContainerType type) {
		return content.getBranches(type);
	}

	@Override
	public List<FieldContainerChange> compareTo(HibNodeFieldContainer content, HibNodeFieldContainer container) {
		return content.compareTo(container);
	}

	@Override
	public List<FieldContainerChange> compareTo(HibNodeFieldContainer content, FieldMap fieldMap) {
		return content.compareTo(fieldMap);
	}

	@Override
	public HibSchemaVersion getSchemaContainerVersion(HibNodeFieldContainer content) {
		return content.getSchemaContainerVersion();
	}

	@Override
	public List<HibMicronodeField> getMicronodeFields(HibNodeFieldContainer content, HibMicroschemaVersion version) {
		return content.getMicronodeFields(version);
	}

	@Override
	public Result<HibMicronodeFieldList> getMicronodeListFields(HibNodeFieldContainer content, HibMicroschemaVersion version) {
		return content.getMicronodeListFields(version);
	}

	@Override
	public String getETag(HibNodeFieldContainer content, InternalActionContext ac) {
		return content.getETag(ac);
	}

	@Override
	public void updateDisplayFieldValue(HibNodeFieldContainer content) {
		content.updateDisplayFieldValue();
	}

	@Override
	public String getSegmentFieldValue(HibNodeFieldContainer content) {
		return content.getSegmentFieldValue();
	}

	@Override
	public void postfixSegmentFieldValue(HibNodeFieldContainer content) {
		content.postfixSegmentFieldValue();
	}

	@Override
	public Stream<String> getUrlFieldValues(HibNodeFieldContainer content) {
		return content.getUrlFieldValues();
	}

	@Override
	public Path getPath(HibNodeFieldContainer content, InternalActionContext ac) {
		return content.getPath(ac);
	}

	@Override
	public Iterator<GraphFieldContainerEdge> getContainerEdge(HibNodeFieldContainer content, ContainerType type, String branchUuid) {
		return toGraph(content).getContainerEdge(type, branchUuid);
	}

	@Override
	public NodeMeshEventModel onDeleted(HibNodeFieldContainer content, String branchUuid, ContainerType type) {
		return content.onDeleted(branchUuid, type);
	}

	@Override
	public NodeMeshEventModel onCreated(HibNodeFieldContainer content, String branchUuid, ContainerType type) {
		return content.onCreated(branchUuid, type);
	}

	@Override
	public NodeMeshEventModel onUpdated(HibNodeFieldContainer content, String branchUuid, ContainerType type) {
		return content.onUpdated(branchUuid, type);
	}

	@Override
	public NodeMeshEventModel onTakenOffline(HibNodeFieldContainer content, String branchUuid) {
		return content.onTakenOffline(branchUuid);
	}

	@Override
	public NodeMeshEventModel onPublish(HibNodeFieldContainer content, String branchUuid) {
		return content.onPublish(branchUuid);
	}

	@Override
	public VersionInfo transformToVersionInfo(HibNodeFieldContainer content, InternalActionContext ac) {
		return content.transformToVersionInfo(ac);
	}

	@Override
	public boolean isPurgeable(HibNodeFieldContainer content) {
		return content.isPurgeable();
	}

	@Override
	public boolean isAutoPurgeEnabled(HibNodeFieldContainer content) {
		return content.isAutoPurgeEnabled();
	}

	@Override
	public void purge(HibNodeFieldContainer content, BulkActionContext bac) {
		content.purge(bac);
	}

	@Override
	public Result<HibNodeFieldContainer> versions(HibNodeFieldContainer content) {
		return content.versions();
	}

	@Override
	public String getLanguageTag(HibNodeFieldContainer content) {
		return content.getLanguageTag();
	}

	@Override
	public void setLanguageTag(HibNodeFieldContainer content, String languageTag) {
		content.setLanguageTag(languageTag);
	}

	@Override
	public long globalCount() {
		return HibClassConverter.toGraph(db).count(NodeGraphFieldContainer.class);
	}
}
