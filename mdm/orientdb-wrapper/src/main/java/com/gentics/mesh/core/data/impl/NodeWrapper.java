package com.gentics.mesh.core.data.impl;

import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Stream;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeTaggedEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.path.Path;
import com.google.gson.JsonObject;
import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

import io.reactivex.Single;
import io.vertx.core.Vertx;

public class NodeWrapper implements Node {

	private final Node delegate;

	public static NodeWrapper wrap(Node node) {
		if (node == null) {
			return null;
		} else {
			return new NodeWrapper(node);
		}
	}

	public static TraversalResult<? extends NodeWrapper> wrap(TraversalResult<? extends Node> nodes) {
		return new TraversalResult<>(nodes.stream().map(NodeWrapper::new));
	}

	public NodeWrapper(Node delegate) {
		this.delegate = delegate;
	}

	public Object id() {
		return delegate.id();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return delegate.getRolePermissions(ac, roleUuid);
	}

	public HibUser getCreator() {
		return delegate.getCreator();
	}

	public void setUuid(String uuid) {
		delegate.setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		delegate.setUniqueLinkOutTo(vertex, labels);
	}

	public String getAPIPath(InternalActionContext ac) {
		return delegate.getAPIPath(ac);
	}

	public void setCreator(HibUser user) {
		delegate.setCreator(user);
	}

	public TraversalResult<? extends HibRole> getRolesWithPerm(InternalPermission perm) {
		return delegate.getRolesWithPerm(perm);
	}

	public String getUuid() {
		return delegate.getUuid();
	}

	public Single<NodeResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return delegate.transformToRest(ac, level, languageTags);
	}

	public Vertex getVertex() {
		return delegate.getVertex();
	}

	public String getElementVersion() {
		return delegate.getElementVersion();
	}

	public Long getCreationTimestamp() {
		return delegate.getCreationTimestamp();
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		delegate.setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return delegate.property(name);
	}

	public void delete(BulkActionContext bac) {
		delegate.delete(bac);
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

	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		return delegate.update(ac, batch);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		delegate.setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return delegate.getPropertyKeys();
	}

	public String getCreationDate() {
		return delegate.getCreationDate();
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

	public HibProject getProject() {
		return delegate.getProject();
	}

	public void delete() {
		delegate.delete();
	}

	public NodeResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		return delegate.transformToRestSync(ac, level, languageTags);
	}

	public void setCreationTimestamp(long timestamp) {
		delegate.setCreationTimestamp(timestamp);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return delegate.out(label, clazz);
	}

	public FramedGraph getGraph() {
		return delegate.getGraph();
	}

	public List<HibTag> getTagsToSet(InternalActionContext ac, EventQueueBatch batch) {
		return delegate.getTagsToSet(ac, batch);
	}

	public <R> void property(String key, R value) {
		delegate.property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
		delegate.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return delegate.outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return delegate.getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return delegate.in(label, clazz);
	}

	public void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model) {
		delegate.fillCommonRestFields(ac, fields, model);
	}

	public void setCreationTimestamp() {
		delegate.setCreationTimestamp();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return delegate.addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		delegate.removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
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

	public void setCreated(User creator) {
		delegate.setCreated(creator);
	}

	public String getETag(InternalActionContext ac) {
		return delegate.getETag(ac);
	}

	public Vertx vertx() {
		return delegate.vertx();
	}

	public List<HibTag> getTagsToSet(List<TagReference> list, InternalActionContext ac, EventQueueBatch batch) {
		return delegate.getTagsToSet(list, ac, batch);
	}

	public MeshOptions options() {
		return delegate.options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return delegate.addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public MeshElementEventModel onCreated() {
		return delegate.onCreated();
	}

	public void setCachedUuid(String uuid) {
		delegate.setCachedUuid(uuid);
	}

	public void setProperty(String name, Object value) {
		delegate.setProperty(name, value);
	}

	public MeshElementEventModel onUpdated() {
		return delegate.onUpdated();
	}

	public MeshElementEventModel onDeleted() {
		return delegate.onDeleted();
	}

	public Class<?> getTypeResolution() {
		return delegate.getTypeResolution();
	}

	public PermissionChangedEventModelImpl onPermissionChanged(Role role) {
		return delegate.onPermissionChanged(role);
	}

	public void setTypeResolution(Class<?> type) {
		delegate.setTypeResolution(type);
	}

	public void fillPermissionChanged(PermissionChangedEventModelImpl model, Role role) {
		delegate.fillPermissionChanged(model, role);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return delegate.addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		delegate.removeTypeResolution();
	}

	public VertexTraversal<?, ?, ?> v() {
		return delegate.v();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return delegate.e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return delegate.e(ids);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return delegate.addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return delegate.getGraphAttribute(key);
	}

	public TypeInfo getTypeInfo() {
		return delegate.getTypeInfo();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return delegate.in(labels);
	}

	public void addTag(HibTag tag, HibBranch branch) {
		delegate.addTag(tag, branch);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return delegate.outE(labels);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return delegate.inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.linkOut(vertex, labels);
	}

	public void removeTag(HibTag tag, HibBranch branch) {
		delegate.removeTag(tag, branch);
	}

	public void removeAllTags(HibBranch branch) {
		delegate.removeAllTags(branch);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.linkIn(vertex, labels);
	}

	public TraversalResult<HibTag> getTags(HibBranch branch) {
		return delegate.getTags(branch);
	}

	public TransformablePage<? extends HibTag> getTags(HibUser user, PagingParameters params, HibBranch branch) {
		return delegate.getTags(user, params, branch);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.unlinkOut(vertex, labels);
	}

	public boolean hasTag(HibTag tag, HibBranch branch) {
		return delegate.hasTag(tag, branch);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.unlinkIn(vertex, labels);
	}

	public NodeGraphFieldContainer getLatestDraftFieldContainer(String languageTag) {
		return delegate.getLatestDraftFieldContainer(languageTag);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.setLinkOut(vertex, labels);
	}

	public ContentWrapper getGraphFieldContainer(String languageTag, HibBranch branch, ContainerType type) {
		return ContentWrapper.wrap(delegate.getGraphFieldContainer(languageTag, branch, type));
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return delegate.traversal();
	}

	public ContentWrapper getGraphFieldContainer(String languageTag) {
		return ContentWrapper.wrap(delegate.getGraphFieldContainer(languageTag));
	}

	public JsonObject toJson() {
		return delegate.toJson();
	}

	public ContentWrapper getGraphFieldContainer(String languageTag, String branchUuid, ContainerType type) {
		return ContentWrapper.wrap(delegate.getGraphFieldContainer(languageTag, branchUuid, type));
	}

	public <T> T reframe(Class<T> kind) {
		return delegate.reframe(kind);
	}

	public ContentWrapper createGraphFieldContainer(String languageTag, HibBranch branch, HibUser user) {
		return ContentWrapper.wrap(delegate.createGraphFieldContainer(languageTag, branch, user));
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return delegate.reframeExplicit(kind);
	}

	public NodeGraphFieldContainer createGraphFieldContainer(String languageTag, HibBranch branch, HibUser editor, NodeGraphFieldContainer original,
		boolean handleDraftEdge) {
		return delegate.createGraphFieldContainer(languageTag, branch, editor, original, handleDraftEdge);
	}

	public TraversalResult<NodeGraphFieldContainer> getDraftGraphFieldContainers() {
		return delegate.getDraftGraphFieldContainers();
	}

	public TraversalResult<NodeGraphFieldContainer> getGraphFieldContainers(Branch branch, ContainerType type) {
		return delegate.getGraphFieldContainers(branch, type);
	}

	public TraversalResult<NodeGraphFieldContainer> getGraphFieldContainers(String branchUuid, ContainerType type) {
		return delegate.getGraphFieldContainers(branchUuid, type);
	}

	public TraversalResult<NodeGraphFieldContainer> getGraphFieldContainers(ContainerType type) {
		return delegate.getGraphFieldContainers(type);
	}

	public long getGraphFieldContainerCount() {
		return delegate.getGraphFieldContainerCount();
	}

	public List<String> getAvailableLanguageNames() {
		return delegate.getAvailableLanguageNames();
	}

	public void setProject(HibProject project) {
		delegate.setProject(project);
	}

	public TraversalResult<Node> getChildren() {
		return delegate.getChildren();
	}

	public TraversalResult<Node> getChildren(String branchUuid) {
		return delegate.getChildren(branchUuid);
	}

	public Stream<Node> getChildrenStream(InternalActionContext ac) {
		return delegate.getChildrenStream(ac).map(NodeWrapper::new);
	}

	public NodeWrapper getParentNode(String branchUuid) {
		return wrap(delegate.getParentNode(branchUuid));
	}

	public void setParentNode(String branchUuid, Node parentNode) {
		delegate.setParentNode(branchUuid, parentNode);
	}

	public NodeWrapper create(HibUser creator, SchemaVersion schemaVersion, HibProject project) {
		return wrap(delegate.create(creator, schemaVersion, project));
	}

	public NodeWrapper create(HibUser creator, SchemaVersion schemaVersion, HibProject project, HibBranch branch) {
		return wrap(delegate.create(creator, schemaVersion, project, branch));
	}

	public NodeWrapper create(HibUser creator, SchemaVersion schemaVersion, HibProject project, HibBranch branch, String uuid) {
		return wrap(delegate.create(creator, schemaVersion, project, branch, uuid));
	}

	public TransformablePage<Node> getChildren(InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type,
		PagingParameters pagingParameter) {
		return delegate.getChildren(ac, languageTags, branchUuid, type, pagingParameter);
	}

	public String getDisplayName(InternalActionContext ac) {
		return delegate.getDisplayName(ac);
	}

	public ContentWrapper findVersion(List<String> languageTags, String branchUuid, String version) {
		return ContentWrapper.wrap(delegate.findVersion(languageTags, branchUuid, version));
	}

	public ContentWrapper findVersion(String languageTag, String branchUuid, String version) {
		return ContentWrapper.wrap(delegate.findVersion(languageTag, branchUuid, version));
	}

	public ContentWrapper findVersion(InternalActionContext ac, List<String> languageTags, String version) {
		return ContentWrapper.wrap(delegate.findVersion(ac, languageTags, version));
	}

	public ContentWrapper findVersion(InternalActionContext ac, List<String> languageTags, ContainerType type) {
		return ContentWrapper.wrap(delegate.findVersion(ac, languageTags, type));
	}

	public void moveTo(InternalActionContext ac, Node targetNode, EventQueueBatch batch) {
		delegate.moveTo(ac, targetNode, batch);
	}

	public NodeReference transformToReference(InternalActionContext ac) {
		return delegate.transformToReference(ac);
	}

	public NavigationResponse transformToNavigation(InternalActionContext ac) {
		return delegate.transformToNavigation(ac);
	}

	public PublishStatusResponse transformToPublishStatus(InternalActionContext ac) {
		return delegate.transformToPublishStatus(ac);
	}

	public void publish(InternalActionContext ac, BulkActionContext bac) {
		delegate.publish(ac, bac);
	}

	public void takeOffline(InternalActionContext ac, BulkActionContext bac) {
		delegate.takeOffline(ac, bac);
	}

	public PublishStatusModel transformToPublishStatus(InternalActionContext ac, String languageTag) {
		return delegate.transformToPublishStatus(ac, languageTag);
	}

	public void publish(InternalActionContext ac, BulkActionContext bac, String languageTag) {
		delegate.publish(ac, bac, languageTag);
	}

	public void setPublished(InternalActionContext ac, NodeGraphFieldContainer container, String branchUuid) {
		delegate.setPublished(ac, container, branchUuid);
	}

	public void takeOffline(InternalActionContext ac, BulkActionContext bac, HibBranch branch, String languageTag) {
		delegate.takeOffline(ac, bac, branch, languageTag);
	}

	public void deleteLanguageContainer(InternalActionContext ac, HibBranch branch, String languageTag, BulkActionContext bac,
		boolean failForLastContainer) {
		delegate.deleteLanguageContainer(ac, branch, languageTag, bac, failForLastContainer);
	}

	public Path resolvePath(String branchUuid, ContainerType type, Path nodePath, Stack<String> pathStack) {
		return delegate.resolvePath(branchUuid, type, nodePath, pathStack);
	}

	public String getPath(ActionContext ac, String branchUuid, ContainerType type, String... languageTag) {
		return delegate.getPath(ac, branchUuid, type, languageTag);
	}

	public String getPathSegment(String branchUuid, ContainerType type, boolean anyLanguage, String... languageTag) {
		return delegate.getPathSegment(branchUuid, type, anyLanguage, languageTag);
	}

	public String getPathSegment(String branchUuid, ContainerType type, String... languageTag) {
		return delegate.getPathSegment(branchUuid, type, languageTag);
	}

	public void deleteFromBranch(InternalActionContext ac, HibBranch branch, BulkActionContext bac, boolean ignoreChecks) {
		delegate.deleteFromBranch(ac, branch, bac, ignoreChecks);
	}

	public SchemaWrapper getSchemaContainer() {
		return SchemaWrapper.wrap(delegate.getSchemaContainer());
	}

	public void setSchemaContainer(Schema container) {
		delegate.setSchemaContainer(container);
	}

	public void delete(BulkActionContext bac, boolean ignoreChecks, boolean recusive) {
		delegate.delete(bac, ignoreChecks, recusive);
	}

	public TransformablePage<? extends HibTag> updateTags(InternalActionContext ac, EventQueueBatch batch) {
		return delegate.updateTags(ac, batch);
	}

	public void updateTags(InternalActionContext ac, EventQueueBatch batch, List<TagReference> list) {
		delegate.updateTags(ac, batch, list);
	}

	public TraversalResult<Node> getBreadcrumbNodes(InternalActionContext ac) {
		return delegate.getBreadcrumbNodes(ac);
	}

	public NodeMeshEventModel onDeleted(String uuid, Schema schema, String branchUuid, ContainerType type, String languageTag) {
		return delegate.onDeleted(uuid, schema, branchUuid, type, languageTag);
	}

	public NodeTaggedEventModel onTagged(HibTag tag, HibBranch branch, Assignment assignment) {
		return delegate.onTagged(tag, branch, assignment);
	}

	public com.syncleus.ferma.EdgeFrame getGraphFieldContainerEdgeFrame(String languageTag, String branchUuid, ContainerType type) {
		return delegate.getGraphFieldContainerEdgeFrame(languageTag, branchUuid, type);
	}

	public boolean isBaseNode() {
		return delegate.isBaseNode();
	}

	public boolean isVisibleInBranch(String branchUuid) {
		return delegate.isVisibleInBranch(branchUuid);
	}

	public NodeReference transformToMinimalReference() {
		return delegate.transformToMinimalReference();
	}

	public boolean hasPublishPermissions() {
		return delegate.hasPublishPermissions();
	}

	public NodeVersionsResponse transformToVersionList(InternalActionContext ac) {
		return delegate.transformToVersionList(ac);
	}

	public Stream<NodeGraphField> getInboundReferences() {
		return delegate.getInboundReferences();
	}

	public Set<String> getRoleUuidsForPerm(InternalPermission permission) {
		return delegate.getRoleUuidsForPerm(permission);
	}

	public void setRoleUuidForPerm(InternalPermission permission, Set<String> allowedRoles) {
		delegate.setRoleUuidForPerm(permission, allowedRoles);
	}

	@Override
	public NodeGraphFieldContainer publish(InternalActionContext ac, String languageTag, HibBranch branch, HibUser user) {
		return delegate.publish(ac, languageTag, branch, user);
	}
}
