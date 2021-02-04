package com.gentics.mesh.core.data.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibContent;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibListField;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.util.VersionNumber;
import com.google.gson.JsonObject;
import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.Vertx;

public class ContentWrapper implements NodeGraphFieldContainer, HibContent {

	private final NodeGraphFieldContainer delegate;

	public ContentWrapper(NodeGraphFieldContainer delegate) {
		this.delegate = delegate;
	}

	public static ContentWrapper wrap(NodeGraphFieldContainer container) {
		if (container == null) {
			return null;
		} else {
			return new ContentWrapper(container);
		}
	}

	public String getLanguageTag() {
		return delegate.getLanguageTag();
	}

	public Object id() {
		return delegate.id();
	}

	public void setLanguageTag(String languageTag) {
		delegate.setLanguageTag(languageTag);
	}

	public HibUser getEditor() {
		return delegate.getEditor();
	}

	public void setUuid(String uuid) {
		delegate.setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		delegate.setUniqueLinkOutTo(vertex, labels);
	}

	public void setEditor(HibUser user) {
		delegate.setEditor(user);
	}

	public String getUuid() {
		return delegate.getUuid();
	}

	public Vertex getVertex() {
		return delegate.getVertex();
	}

	public String getElementVersion() {
		return delegate.getElementVersion();
	}

	public Long getLastEditedTimestamp() {
		return delegate.getLastEditedTimestamp();
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		delegate.setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return delegate.property(name);
	}

	public Vertex getElement() {
		return delegate.getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		delegate.setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return delegate.getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return delegate.addFramedEdge(label, inVertex, initializer);
	}

	public void setLastEditedTimestamp(long timestamp) {
		delegate.setLastEditedTimestamp(timestamp);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		delegate.setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return delegate.getPropertyKeys();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		delegate.addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return delegate.out(labels);
	}

	public void remove() {
		delegate.remove();
	}

	public void delete() {
		delegate.delete();
	}

	public <T extends ElementFrame> Result<? extends T> out(String label, Class<T> clazz) {
		return delegate.out(label, clazz);
	}

	public void setLastEditedTimestamp() {
		delegate.setLastEditedTimestamp();
	}

	public FramedGraph getGraph() {
		return delegate.getGraph();
	}

	public <R> void property(String key, R value) {
		delegate.property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
		delegate.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> Result<? extends T> outE(String label, Class<T> clazz) {
		return delegate.outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return delegate.getProperty(name);
	}

	public <T extends ElementFrame> Result<? extends T> in(String label, Class<T> clazz) {
		return delegate.in(label, clazz);
	}

	public String getLastEditedDate() {
		return delegate.getLastEditedDate();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return delegate.addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		delegate.removeProperty(key);
	}

	public <T extends EdgeFrame> Result<? extends T> inE(String label, Class<T> clazz) {
		return delegate.inE(label, clazz);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return delegate.traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return delegate.getProperty(name, type);
	}

	public Database db() {
		return delegate.db();
	}

	public Vertx vertx() {
		return delegate.vertx();
	}

	public boolean hasPublishPermissions() {
		return delegate.hasPublishPermissions();
	}

	public MeshOptions options() {
		return delegate.options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return delegate.addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public void setCachedUuid(String uuid) {
		delegate.setCachedUuid(uuid);
	}

	public void setProperty(String name, Object value) {
		delegate.setProperty(name, value);
	}

	public HibStringField getString(String key) {
		return delegate.getString(key);
	}

	public HibStringField createString(String key) {
		return delegate.createString(key);
	}

	public Class<?> getTypeResolution() {
		return delegate.getTypeResolution();
	}

	public HibBinaryField getBinary(String key) {
		return delegate.getBinary(key);
	}

	public void setTypeResolution(Class<?> type) {
		delegate.setTypeResolution(type);
	}

	public HibBinaryField createBinary(String fieldKey, HibBinary binary) {
		return delegate.createBinary(fieldKey, binary);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return delegate.addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		delegate.removeTypeResolution();
	}

	public HibNodeField getNode(String key) {
		return delegate.getNode(key);
	}

	public VertexTraversal<?, ?, ?> v() {
		return delegate.v();
	}

	public HibNodeField createNode(String key, HibNode node) {
		return delegate.createNode(key, node);
	}

	public EdgeTraversal<?, ?, ?> e() {
		return delegate.e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return delegate.e(ids);
	}

	public HibDateField getDate(String key) {
		return delegate.getDate(key);
	}

	public HibDateField createDate(String key) {
		return delegate.createDate(key);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return delegate.addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return delegate.getGraphAttribute(key);
	}

	public HibNumberField getNumber(String key) {
		return delegate.getNumber(key);
	}

	public HibNumberField createNumber(String key) {
		return delegate.createNumber(key);
	}

	public HibHtmlField getHtml(String key) {
		return delegate.getHtml(key);
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return delegate.in(labels);
	}

	public HibHtmlField createHTML(String key) {
		return delegate.createHTML(key);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return delegate.outE(labels);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return delegate.inE(labels);
	}

	public HibBooleanField getBoolean(String key) {
		return delegate.getBoolean(key);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.linkOut(vertex, labels);
	}

	public HibBooleanField createBoolean(String key) {
		return delegate.createBoolean(key);
	}

	public HibMicronodeField getMicronode(String key) {
		return delegate.getMicronode(key);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.linkIn(vertex, labels);
	}

	public HibMicronodeField createMicronode(String key, HibMicroschemaVersion microschemaVersion) {
		return delegate.createMicronode(key, microschemaVersion);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.unlinkOut(vertex, labels);
	}

	public HibDateFieldList getDateList(String fieldKey) {
		return delegate.getDateList(fieldKey);
	}

	public HibDateFieldList createDateList(String fieldKey) {
		return delegate.createDateList(fieldKey);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.unlinkIn(vertex, labels);
	}

	public HibHtmlFieldList getHTMLList(String fieldKey) {
		return delegate.getHTMLList(fieldKey);
	}

	public HibHtmlFieldList createHTMLList(String fieldKey) {
		return delegate.createHTMLList(fieldKey);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.setLinkOut(vertex, labels);
	}

	public HibNumberFieldList getNumberList(String fieldKey) {
		return delegate.getNumberList(fieldKey);
	}

	public HibNumberFieldList createNumberList(String fieldKey) {
		return delegate.createNumberList(fieldKey);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return delegate.traversal();
	}

	public HibNodeFieldList getNodeList(String fieldKey) {
		return delegate.getNodeList(fieldKey);
	}

	public JsonObject toJson() {
		return delegate.toJson();
	}

	public HibNodeFieldList createNodeList(String fieldKey) {
		return delegate.createNodeList(fieldKey);
	}

	public <T> T reframe(Class<T> kind) {
		return delegate.reframe(kind);
	}

	public HibStringFieldList getStringList(String fieldKey) {
		return delegate.getStringList(fieldKey);
	}

	public HibStringFieldList createStringList(String fieldKey) {
		return delegate.createStringList(fieldKey);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return delegate.reframeExplicit(kind);
	}

	public HibBooleanFieldList getBooleanList(String fieldKey) {
		return delegate.getBooleanList(fieldKey);
	}

	public HibBooleanFieldList createBooleanList(String fieldKey) {
		return delegate.createBooleanList(fieldKey);
	}

	public String getIndexName(String projectUuid, String branchUuid, ContainerType type) {
		return delegate.getIndexName(projectUuid, branchUuid, type);
	}

	public HibMicronodeFieldList getMicronodeList(String fieldKey) {
		return delegate.getMicronodeList(fieldKey);
	}

	public HibMicronodeFieldList createMicronodeFieldList(String fieldKey) {
		return delegate.createMicronodeFieldList(fieldKey);
	}

	public Field getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, List<String> languageTags, int level) {
		return delegate.getRestFieldFromGraph(ac, fieldKey, fieldSchema, languageTags, level);
	}

	public void updateFieldsFromRest(InternalActionContext ac, FieldMap restFields) {
		delegate.updateFieldsFromRest(ac, restFields);
	}

	public String getDocumentId() {
		return delegate.getDocumentId();
	}

	public HibField getField(FieldSchema fieldSchema) {
		return delegate.getField(fieldSchema);
	}

	public List<HibField> getFields() {
		return delegate.getFields();
	}

	public void delete(BulkActionContext bac) {
		delegate.delete(bac);
	}

	public <T extends HibListField<?, ?, ?>> T getList(Class<T> classOfT, String fieldKey) {
		return delegate.getList(classOfT, fieldKey);
	}

	public void delete(BulkActionContext bac, boolean deleteNext) {
		delegate.delete(bac, deleteNext);
	}

	public void validate() {
		delegate.validate();
	}

	public void deleteFieldEdge(String key) {
		delegate.deleteFieldEdge(key);
	}

	public void deleteFromBranch(HibBranch branch, BulkActionContext bac) {
		delegate.deleteFromBranch(branch, bac);
	}

	public void setSchemaContainerVersion(HibFieldSchemaVersionElement version) {
		delegate.setSchemaContainerVersion(version);
	}

	public String getDisplayFieldValue() {
		return delegate.getDisplayFieldValue();
	}

	public HibNode getNode() {
		return delegate.getNode();
	}

	public Iterable<? extends HibNode> getReferencedNodes() {
		return delegate.getReferencedNodes();
	}

	public void updateWebrootPathInfo(InternalActionContext ac, String branchUuid, String conflictI18n) {
		delegate.updateWebrootPathInfo(ac, branchUuid, conflictI18n);
	}

	public Stream<? extends HibNodeFieldContainer> getContents() {
		return delegate.getContents();
	}

	public void updateWebrootPathInfo(String branchUuid, String conflictI18n) {
		delegate.updateWebrootPathInfo(branchUuid, conflictI18n);
	}

	public VersionNumber getVersion() {
		return delegate.getVersion();
	}

	public void setVersion(VersionNumber version) {
		delegate.setVersion(version);
	}

	public boolean hasNextVersion() {
		return delegate.hasNextVersion();
	}

	public Iterable<HibNodeFieldContainer> getNextVersions() {
		return delegate.getNextVersions();
	}

	public void setNextVersion(HibNodeFieldContainer container) {
		delegate.setNextVersion(container);
	}

	public boolean hasPreviousVersion() {
		return delegate.hasPreviousVersion();
	}

	public HibNodeFieldContainer getPreviousVersion() {
		return delegate.getPreviousVersion();
	}

	public void clone(HibNodeFieldContainer container) {
		delegate.clone(container);
	}

	public boolean isInitial() {
		return delegate.isInitial();
	}

	public boolean isDraft() {
		return delegate.isDraft();
	}

	public boolean isPublished() {
		return delegate.isPublished();
	}

	public boolean isType(ContainerType type) {
		return delegate.isType(type);
	}

	public boolean isInitial(String branchUuid) {
		return delegate.isInitial(branchUuid);
	}

	public boolean isDraft(String branchUuid) {
		return delegate.isDraft(branchUuid);
	}

	public boolean isPublished(String branchUuid) {
		return delegate.isPublished(branchUuid);
	}

	public boolean isType(ContainerType type, String branchUuid) {
		return delegate.isType(type, branchUuid);
	}

	public Set<String> getBranches(ContainerType type) {
		return delegate.getBranches(type);
	}

	public List<FieldContainerChange> compareTo(HibNodeFieldContainer container) {
		return delegate.compareTo(container);
	}

	public List<FieldContainerChange> compareTo(FieldMap fieldMap) {
		return delegate.compareTo(fieldMap);
	}

	public HibSchemaVersion getSchemaContainerVersion() {
		return delegate.getSchemaContainerVersion();
	}

	public List<HibMicronodeField> getMicronodeFields(HibMicroschemaVersion version) {
		return delegate.getMicronodeFields(version);
	}

	public Result<HibMicronodeFieldList> getMicronodeListFields(HibMicroschemaVersion version) {
		return delegate.getMicronodeListFields(version);
	}

	public String getETag(InternalActionContext ac) {
		return delegate.getETag(ac);
	}

	public void updateDisplayFieldValue() {
		delegate.updateDisplayFieldValue();
	}

	public String getSegmentFieldValue() {
		return delegate.getSegmentFieldValue();
	}

	public void postfixSegmentFieldValue() {
		delegate.postfixSegmentFieldValue();
	}

	public Stream<String> getUrlFieldValues() {
		return delegate.getUrlFieldValues();
	}

	public Path getPath(InternalActionContext ac) {
		return delegate.getPath(ac);
	}

	public Iterator<GraphFieldContainerEdge> getContainerEdge(ContainerType type, String branchUuid) {
		return delegate.getContainerEdge(type, branchUuid);
	}

	public NodeMeshEventModel onDeleted(String branchUuid, ContainerType type) {
		return delegate.onDeleted(branchUuid, type);
	}

	public NodeMeshEventModel onCreated(String branchUuid, ContainerType type) {
		return delegate.onCreated(branchUuid, type);
	}

	public NodeMeshEventModel onUpdated(String branchUuid, ContainerType type) {
		return delegate.onUpdated(branchUuid, type);
	}

	public NodeMeshEventModel onTakenOffline(String branchUuid) {
		return delegate.onTakenOffline(branchUuid);
	}

	public NodeMeshEventModel onPublish(String branchUuid) {
		return delegate.onPublish(branchUuid);
	}

	public VersionInfo transformToVersionInfo(InternalActionContext ac) {
		return delegate.transformToVersionInfo(ac);
	}

	public boolean isPurgeable() {
		return delegate.isPurgeable();
	}

	public boolean isAutoPurgeEnabled() {
		return delegate.isAutoPurgeEnabled();
	}

	public void purge(BulkActionContext bac) {
		delegate.purge(bac);
	}

	public void purge() {
		delegate.purge();
	}

	public Result<HibNodeFieldContainer> versions() {
		return delegate.versions();
	}

	public Set<String> getRoleUuidsForPerm(InternalPermission permission) {
		return delegate.getRoleUuidsForPerm(permission);
	}

	public void setRoleUuidForPerm(InternalPermission permission, Set<String> allowedRoles) {
		delegate.setRoleUuidForPerm(permission, allowedRoles);
	}

	@Override
	public void setBucketId(Integer bucketId) {
		delegate.setBucketId(bucketId);
	}

	@Override
	public Integer getBucketId() {
		return delegate.getBucketId();
	}

	@Override
	public void generateBucketId() {
		delegate.generateBucketId();
	}

}
