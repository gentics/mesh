package com.gentics.mesh.core.data.impl;

import java.util.Set;
import java.util.function.Function;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.google.gson.JsonObject;
import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

import io.reactivex.Single;
import io.vertx.core.Vertx;

public class RoleWrapper implements Role {

	private final Role delete;

	public RoleWrapper(Role delegate) {
		this.delete = delegate;
	}

	public Object id() {
		return delete.id();
	}

	public void setCreated(User creator) {
		delete.setCreated(creator);
	}

	public RoleReference transformToReference() {
		return delete.transformToReference();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return delete.getRolePermissions(ac, roleUuid);
	}

	public User getEditor() {
		return delete.getEditor();
	}

	public User getCreator() {
		return delete.getCreator();
	}

	public void setUuid(String uuid) {
		delete.setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		delete.setUniqueLinkOutTo(vertex, labels);
	}

	public String getName() {
		return delete.getName();
	}

	public String getAPIPath(InternalActionContext ac) {
		return delete.getAPIPath(ac);
	}

	public void setEditor(User user) {
		delete.setEditor(user);
	}

	public void setCreator(User user) {
		delete.setCreator(user);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return delete.getRolesWithPerm(perm);
	}

	public String getUuid() {
		return delete.getUuid();
	}

	public void setName(String name) {
		delete.setName(name);
	}

	public Single<RoleResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return delete.transformToRest(ac, level, languageTags);
	}

	public Vertex getVertex() {
		return delete.getVertex();
	}

	public String getElementVersion() {
		return delete.getElementVersion();
	}

	public Long getLastEditedTimestamp() {
		return delete.getLastEditedTimestamp();
	}

	public Long getCreationTimestamp() {
		return delete.getCreationTimestamp();
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		delete.setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return delete.property(name);
	}

	public void delete(BulkActionContext bac) {
		delete.delete(bac);
	}

	public Vertex getElement() {
		return delete.getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		delete.setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return delete.getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return delete.addFramedEdge(label, inVertex, initializer);
	}

	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		return delete.update(ac, batch);
	}

	public void setLastEditedTimestamp(long timestamp) {
		delete.setLastEditedTimestamp(timestamp);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		delete.setSingleLinkInTo(vertex, labels);
	}

	public TypeInfo getTypeInfo() {
		return delete.getTypeInfo();
	}

	public Set<String> getPropertyKeys() {
		return delete.getPropertyKeys();
	}

	public String getCreationDate() {
		return delete.getCreationDate();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		delete.addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return delete.out(labels);
	}

	public void remove() {
		delete.remove();
	}

	public void delete() {
		delete.delete();
	}

	public RoleResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		return delete.transformToRestSync(ac, level, languageTags);
	}

	public void setCreationTimestamp(long timestamp) {
		delete.setCreationTimestamp(timestamp);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return delete.out(label, clazz);
	}

	public void setLastEditedTimestamp() {
		delete.setLastEditedTimestamp();
	}

	public FramedGraph getGraph() {
		return delete.getGraph();
	}

	public <R> void property(String key, R value) {
		delete.property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		delete.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return delete.outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return delete.getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return delete.in(label, clazz);
	}

	public String getLastEditedDate() {
		return delete.getLastEditedDate();
	}

	public void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model) {
		delete.fillCommonRestFields(ac, fields, model);
	}

	public void setCreationTimestamp() {
		delete.setCreationTimestamp();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return delete.addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		delete.removeProperty(key);
	}

	public void grantPermissions(MeshVertex vertex, GraphPermission... permissions) {
		delete.grantPermissions(vertex, permissions);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return delete.inE(label, clazz);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return delete.traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return delete.getProperty(name, type);
	}

	public Database db() {
		return delete.db();
	}

	public String getETag(InternalActionContext ac) {
		return delete.getETag(ac);
	}

	public void revokePermissions(MeshVertex vertex, GraphPermission... permissions) {
		delete.revokePermissions(vertex, permissions);
	}

	public Vertx vertx() {
		return delete.vertx();
	}

	public boolean hasPublishPermissions() {
		return delete.hasPublishPermissions();
	}

	public MeshOptions options() {
		return delete.options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return delete.addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public Set<GraphPermission> getPermissions(MeshVertex element) {
		return delete.getPermissions(element);
	}

	public MeshElementEventModel onCreated() {
		return delete.onCreated();
	}

	public void setCachedUuid(String uuid) {
		delete.setCachedUuid(uuid);
	}

	public void setProperty(String name, Object value) {
		delete.setProperty(name, value);
	}

	public MeshElementEventModel onUpdated() {
		return delete.onUpdated();
	}

	public TraversalResult<? extends Group> getGroups() {
		return delete.getGroups();
	}

	public MeshElementEventModel onDeleted() {
		return delete.onDeleted();
	}

	public Class<?> getTypeResolution() {
		return delete.getTypeResolution();
	}

	public Page<? extends Group> getGroups(User user, PagingParameters params) {
		return delete.getGroups(user, params);
	}

	public PermissionChangedEventModelImpl onPermissionChanged(Role role) {
		return delete.onPermissionChanged(role);
	}

	public void setTypeResolution(Class<?> type) {
		delete.setTypeResolution(type);
	}

	public boolean hasPermission(GraphPermission permission, MeshVertex element) {
		return delete.hasPermission(permission, element);
	}

	public void fillPermissionChanged(PermissionChangedEventModelImpl model, Role role) {
		delete.fillPermissionChanged(model, role);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return delete.addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		delete.removeTypeResolution();
	}

	public VertexTraversal<?, ?, ?> v() {
		return delete.v();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return delete.e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return delete.e(ids);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return delete.addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return delete.getGraphAttribute(key);
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return delete.in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return delete.outE(labels);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return delete.inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delete.linkOut(vertex, labels);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delete.linkIn(vertex, labels);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delete.unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delete.unlinkIn(vertex, labels);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delete.setLinkOut(vertex, labels);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return delete.traversal();
	}

	public JsonObject toJson() {
		return delete.toJson();
	}

	public <T> T reframe(Class<T> kind) {
		return delete.reframe(kind);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return delete.reframeExplicit(kind);
	}
	
	

}
