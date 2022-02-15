package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibDeletableField;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.VersionNumber;

public class ContentDaoWrapperImpl implements ContentDaoWrapper {

	private final OrientDBDatabase db;

	@Inject
	public ContentDaoWrapperImpl(OrientDBDatabase db) {
		this.db = db;
	}

	@Override
	public HibNodeFieldContainer getLatestDraftFieldContainer(HibNode node, String languageTag) {
		return toGraph(node).getLatestDraftFieldContainer(languageTag);
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
	public Result<? extends HibNodeFieldContainerEdge> getFieldEdges(HibNode node, String branchUuid, ContainerType type) {
		return toGraph(node).getFieldContainerEdges(branchUuid, type);
	}

	@Override
	public long getFieldContainerCount(HibNode node) {
		return toGraph(node).getFieldContainerCount();
	}

	@Override
	public Stream<HibNodeField> getInboundReferences(HibNode node) {
		return toGraph(node).getInboundReferences();
	}

	@Override
	public void delete(HibNodeFieldContainer content, BulkActionContext bac) {
		toGraph(content).delete(bac);
	}

	@Override
	public void delete(HibNodeFieldContainer content, BulkActionContext bac, boolean deleteNext) {
		toGraph(content).delete(bac, deleteNext);
	}

	@Override
	public void deleteFromBranch(HibNodeFieldContainer content, HibBranch branch, BulkActionContext bac) {
		toGraph(content).deleteFromBranch(branch, bac);
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
		return toGraph(content).isType(type);
	}

	@Override
	public boolean isType(HibNodeFieldContainer content, ContainerType type, String branchUuid) {
		return toGraph(content).isType(type, branchUuid);
	}

	@Override
	public Set<String> getBranches(HibNodeFieldContainer content, ContainerType type) {
		return toGraph(content).getBranches(type);
	}

	@Override
	public HibSchemaVersion getSchemaContainerVersion(HibNodeFieldContainer content) {
		return toGraph(content).getSchemaContainerVersion();
	}

	@Override
	public List<HibMicronodeField> getMicronodeFields(HibNodeFieldContainer content, HibMicroschemaVersion version) {
		return toGraph(content).getMicronodeFields(version);
	}

	@Override
	public Result<HibMicronodeFieldList> getMicronodeListFields(HibNodeFieldContainer content, HibMicroschemaVersion version) {
		return toGraph(content).getMicronodeListFields(version);
	}

	@Override
	public String getETag(HibNodeFieldContainer content, InternalActionContext ac) {
		return toGraph(content).getETag(ac);
	}

	@Override
	public void postfixSegmentFieldValue(HibNodeFieldContainer content) {
		toGraph(content).postfixSegmentFieldValue();
	}

	@Override
	public Iterator<GraphFieldContainerEdge> getContainerEdges(HibNodeFieldContainer content, ContainerType type, String branchUuid) {
		return toGraph(content).getContainerEdge(type, branchUuid);
	}

	@Override
	public HibNodeFieldContainerEdge getConflictingEdgeOfWebrootPath(HibNodeFieldContainer content, String segmentInfo, String branchUuid, ContainerType type, HibNodeFieldContainerEdge edge) {
		return toGraph(content).getConflictingEdgeOfWebrootPath(segmentInfo, branchUuid, type, toGraph(edge));
	}

	@Override
	public HibNodeFieldContainerEdge getConflictingEdgeOfWebrootField(HibNodeFieldContainer content, HibNodeFieldContainerEdge edge, String urlFieldValue, String branchUuid, ContainerType type) {
		return toGraph(content).getConflictingEdgeOfWebrootField(toGraph(edge), urlFieldValue, branchUuid, type);
	}

	@Override
	public boolean isPurgeable(HibNodeFieldContainer content) {
		return toGraph(content).isPurgeable();
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
		return db.count(NodeGraphFieldContainer.class);
	}

	@Override
	public HibNodeFieldContainer createPersisted(String nodeuuid, HibSchemaVersion version, String uuid) {
		NodeGraphFieldContainerImpl container = GraphDBTx.getGraphTx().getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		if (StringUtils.isNotBlank(uuid)) {
			container.setUuid(uuid);
		}
		container.setSchemaContainerVersion(version);
		return container;
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag, HibBranch branch,
			ContainerType type) {
		return toGraph(node).getFieldContainer(languageTag, branch, type);
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
	public Node getParentNode(HibNodeFieldContainer container, String branchUuid) {
		NodeGraphFieldContainer graphContainer = toGraph(container);
		return graphContainer.inE(HAS_FIELD_CONTAINER).has(
			GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branchUuid).outV().nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public HibNodeFieldContainerEdge createContainerEdge(HibNode node, HibNodeFieldContainer container,
			HibBranch branch, String languageTag, ContainerType type) {
		GraphFieldContainerEdge edge = toGraph(node).addFramedEdge(HAS_FIELD_CONTAINER, toGraph(container), GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(languageTag);
		edge.setBranchUuid(branch.getUuid());
		edge.setType(type);
		return edge;
	}

	@Override
	public void removeEdge(HibNodeFieldContainerEdge edge) {
		toGraph(edge).remove();
	}

	@Override
	public GraphFieldContainerEdge getEdge(HibNode node, String languageTag, String branchUuid, ContainerType type) {
		return toGraph(node).getGraphFieldContainerEdgeFrame(languageTag, branchUuid, type);
	}

	@Override
	public HibNodeFieldContainer getFieldContainerOfEdge(HibNodeFieldContainerEdge edge) {
		return toGraph(edge).inV().nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);
	}

	@Override
	public Stream<? extends HibMicronode> findAllMicronodes() {
		return StreamUtil.toStream(GraphDBTx.getGraphTx().getGraph().v().has(MicronodeImpl.class).frameExplicit(MicronodeImpl.class));
	}

	@Override
	public Stream<? extends GraphFieldContainerEdge> getContainerEdges(HibNodeFieldContainer container) {
		return toGraph(container).getEdges().stream();
	}

	@Override
	public void deleteField(HibDeletableField field) {
		toGraph(field).remove();
	}

	@Override
	public void setDisplayFieldValue(HibNodeFieldContainer container, String value) {
		toGraph(container).property(NodeGraphFieldContainerImpl.DISPLAY_FIELD_PROPERTY_KEY, value);
	}
}
