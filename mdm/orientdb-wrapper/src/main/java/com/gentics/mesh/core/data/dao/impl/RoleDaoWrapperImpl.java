package com.gentics.mesh.core.data.dao.impl;

import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.common.PermissionInfo;
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
import com.google.gson.JsonObject;
import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

import dagger.Lazy;
import io.reactivex.Single;
import io.vertx.core.Vertx;

@Singleton
public class RoleDaoWrapperImpl implements RoleDaoWrapper {

	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public RoleDaoWrapperImpl(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

	public Object id() {
		return boot.get().roleRoot().id();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return boot.get().roleRoot().getRolePermissions(ac, roleUuid);
	}

	public Role create(String name, User creator) {
		return boot.get().roleRoot().create(name, creator);
	}

	public void setUuid(String uuid) {
		boot.get().roleRoot().setUuid(uuid);
	}

	public String getAPIPath(Role element, InternalActionContext ac) {
		return boot.get().roleRoot().getAPIPath(element, ac);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().roleRoot().setUniqueLinkOutTo(vertex, labels);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return boot.get().roleRoot().getRolesWithPerm(perm);
	}

	public String getUuid() {
		return boot.get().roleRoot().getUuid();
	}

	public Single<RoleResponse> transformToRest(Role element, InternalActionContext ac, int level, String... languageTags) {
		return boot.get().roleRoot().transformToRest(element, ac, level, languageTags);
	}

	public Role create(String name, User creator, String uuid) {
		return boot.get().roleRoot().create(name, creator, uuid);
	}

	public Vertex getVertex() {
		return boot.get().roleRoot().getVertex();
	}

	public String getElementVersion() {
		return boot.get().roleRoot().getElementVersion();
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().roleRoot().setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return boot.get().roleRoot().property(name);
	}

	public void delete(BulkActionContext bac) {
		boot.get().roleRoot().delete(bac);
	}

	public Vertex getElement() {
		return boot.get().roleRoot().getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().roleRoot().setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return boot.get().roleRoot().getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().roleRoot().addFramedEdge(label, inVertex, initializer);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().roleRoot().setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return boot.get().roleRoot().getPropertyKeys();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		boot.get().roleRoot().addToStringSetProperty(propertyKey, value);
	}

	public void addRole(Role role) {
		boot.get().roleRoot().addRole(role);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return boot.get().roleRoot().out(labels);
	}

	public void remove() {
		boot.get().roleRoot().remove();
	}

	public void delete() {
		boot.get().roleRoot().delete();
	}

	public void removeRole(Role role) {
		boot.get().roleRoot().removeRole(role);
	}

	public RoleResponse transformToRestSync(Role element, InternalActionContext ac, int level, String... languageTags) {
		return boot.get().roleRoot().transformToRestSync(element, ac, level, languageTags);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return boot.get().roleRoot().out(label, clazz);
	}

	public FramedGraph getGraph() {
		return boot.get().roleRoot().getGraph();
	}

	public <R> void property(String key, R value) {
		boot.get().roleRoot().property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		boot.get().roleRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return boot.get().roleRoot().outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return boot.get().roleRoot().getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return boot.get().roleRoot().in(label, clazz);
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().roleRoot().addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		boot.get().roleRoot().removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return boot.get().roleRoot().inE(label, clazz);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return boot.get().roleRoot().traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return boot.get().roleRoot().getProperty(name, type);
	}

	public Database db() {
		return boot.get().roleRoot().db();
	}

	public String getETag(Role element, InternalActionContext ac) {
		return boot.get().roleRoot().getETag(element, ac);
	}

	public Vertx vertx() {
		return boot.get().roleRoot().vertx();
	}

	public boolean hasPublishPermissions() {
		return boot.get().roleRoot().hasPublishPermissions();
	}

	public MeshOptions options() {
		return boot.get().roleRoot().options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().roleRoot().addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public void setCachedUuid(String uuid) {
		boot.get().roleRoot().setCachedUuid(uuid);
	}

	public void setProperty(String name, Object value) {
		boot.get().roleRoot().setProperty(name, value);
	}

	public TraversalResult<? extends Role> findAll() {
		return boot.get().roleRoot().findAll();
	}

	public Class<?> getTypeResolution() {
		return boot.get().roleRoot().getTypeResolution();
	}

	public void setTypeResolution(Class<?> type) {
		boot.get().roleRoot().setTypeResolution(type);
	}

	public Stream<? extends Role> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return boot.get().roleRoot().findAllStream(ac, permission);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().roleRoot().addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		boot.get().roleRoot().removeTypeResolution();
	}

	public VertexTraversal<?, ?, ?> v() {
		return boot.get().roleRoot().v();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return boot.get().roleRoot().e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return boot.get().roleRoot().e(ids);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return boot.get().roleRoot().addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return boot.get().roleRoot().getGraphAttribute(key);
	}

	public TraversalResult<? extends Role> findAllDynamic() {
		return boot.get().roleRoot().findAllDynamic();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return boot.get().roleRoot().in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return boot.get().roleRoot().outE(labels);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return boot.get().roleRoot().inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().roleRoot().linkOut(vertex, labels);
	}

	public TransformablePage<? extends Role> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().roleRoot().findAll(ac, pagingInfo);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().roleRoot().linkIn(vertex, labels);
	}

	public TransformablePage<? extends Role> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Role> extraFilter) {
		return boot.get().roleRoot().findAll(ac, pagingInfo, extraFilter);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().roleRoot().unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().roleRoot().unlinkIn(vertex, labels);
	}

	public TransformablePage<? extends Role> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().roleRoot().findAllNoPerm(ac, pagingInfo);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().roleRoot().setLinkOut(vertex, labels);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return boot.get().roleRoot().traversal();
	}

	public Role findByName(String name) {
		return boot.get().roleRoot().findByName(name);
	}

	public JsonObject toJson() {
		return boot.get().roleRoot().toJson();
	}

	public <T> T reframe(Class<T> kind) {
		return boot.get().roleRoot().reframe(kind);
	}

	public Role findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return boot.get().roleRoot().findByName(ac, name, perm);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return boot.get().roleRoot().reframeExplicit(kind);
	}

	public Role findByUuid(String uuid) {
		return boot.get().roleRoot().findByUuid(uuid);
	}

	public Role loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return boot.get().roleRoot().loadObjectByUuid(ac, uuid, perm);
	}

	public Role loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return boot.get().roleRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	public Role loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().roleRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	public MeshVertex resolveToElement(Stack<String> stack) {
		return boot.get().roleRoot().resolveToElement(stack);
	}

	public Role create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().roleRoot().create(ac, batch);
	}

	public Role create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().roleRoot().create(ac, batch, uuid);
	}

	public void addItem(Role item) {
		boot.get().roleRoot().addItem(item);
	}

	public void removeItem(Role item) {
		boot.get().roleRoot().removeItem(item);
	}

	public String getRootLabel() {
		return boot.get().roleRoot().getRootLabel();
	}

	public Class<? extends Role> getPersistanceClass() {
		return boot.get().roleRoot().getPersistanceClass();
	}

	public long computeCount() {
		return boot.get().roleRoot().computeCount();
	}

	public PermissionInfo getRolePermissions(MeshVertex vertex, InternalActionContext ac, String roleUuid) {
		return boot.get().roleRoot().getRolePermissions(vertex, ac, roleUuid);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(MeshVertex vertex, GraphPermission perm) {
		return boot.get().roleRoot().getRolesWithPerm(vertex, perm);
	}

	public void delete(Role element, BulkActionContext bac) {
		boot.get().roleRoot().delete(element, bac);
	}

	@Override
	public void grantPermissions(Role role, MeshVertex vertex, GraphPermission... permissions) {
		boot.get().roleRoot().grantPermissions(role, vertex, permissions);
	}

	@Override
	public void revokePermissions(Role role, MeshVertex vertex, GraphPermission... permissions) {
		boot.get().roleRoot().revokePermissions(role, vertex, permissions);
	}

	@Override
	public Set<GraphPermission> getPermissions(Role role, MeshVertex element) {
		return boot.get().roleRoot().getPermissions(role, element);
	}

	@Override
	public Page<? extends Group> getGroups(Role role, User user, PagingParameters params) {
		return boot.get().roleRoot().getGroups(role, user, params);
	}

	@Override
	public boolean update(Role element, InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().roleRoot().update(element, ac, batch);
	}

	@Override
	public boolean hasPermission(Role role, GraphPermission permission, MeshVertex element) {
		return boot.get().roleRoot().hasPermission(role, permission, element);
	}
}
