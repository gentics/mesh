package com.gentics.mesh.core.data.dao.impl;

import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HasPermissions;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.user.UserResponse;
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
import io.vertx.core.Vertx;

public class UserDaoWrapperImpl implements UserDaoWrapper {

	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public UserDaoWrapperImpl(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

	public Object id() {
		return boot.get().userRoot().id();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return boot.get().userRoot().getRolePermissions(ac, roleUuid);
	}

	public void setUuid(String uuid) {
		boot.get().userRoot().setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().userRoot().setUniqueLinkOutTo(vertex, labels);
	}

	public User create(String username, User creator) {
		return boot.get().userRoot().create(username, creator);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return boot.get().userRoot().getRolesWithPerm(perm);
	}

	public String getUuid() {
		return boot.get().userRoot().getUuid();
	}

	public Vertex getVertex() {
		return boot.get().userRoot().getVertex();
	}

	public String getElementVersion() {
		return boot.get().userRoot().getElementVersion();
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().userRoot().setUniqueLinkInTo(vertex, labels);
	}

	public User create(String username, User creator, String uuid) {
		return boot.get().userRoot().create(username, creator, uuid);
	}

	public <T> T property(String name) {
		return boot.get().userRoot().property(name);
	}

	public void delete(BulkActionContext bac) {
		boot.get().userRoot().delete(bac);
	}

	public Vertex getElement() {
		return boot.get().userRoot().getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().userRoot().setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return boot.get().userRoot().getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().userRoot().addFramedEdge(label, inVertex, initializer);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().userRoot().setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return boot.get().userRoot().getPropertyKeys();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		boot.get().userRoot().addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return boot.get().userRoot().out(labels);
	}

	public void remove() {
		boot.get().userRoot().remove();
	}

	public void delete() {
		boot.get().userRoot().delete();
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return boot.get().userRoot().out(label, clazz);
	}

	public FramedGraph getGraph() {
		return boot.get().userRoot().getGraph();
	}

	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		return boot.get().userRoot().findMeshAuthUserByUsername(username);
	}

	public <R> void property(String key, R value) {
		boot.get().userRoot().property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		boot.get().userRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return boot.get().userRoot().outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return boot.get().userRoot().getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return boot.get().userRoot().in(label, clazz);
	}

	public MeshAuthUser findMeshAuthUserByUuid(String userUuid) {
		return boot.get().userRoot().findMeshAuthUserByUuid(userUuid);
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().userRoot().addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		boot.get().userRoot().removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return boot.get().userRoot().inE(label, clazz);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return boot.get().userRoot().traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return boot.get().userRoot().getProperty(name, type);
	}

	public Database db() {
		return boot.get().userRoot().db();
	}

	public User findByUsername(String username) {
		return boot.get().userRoot().findByUsername(username);
	}

	public Vertx vertx() {
		return boot.get().userRoot().vertx();
	}

	public boolean hasPublishPermissions() {
		return boot.get().userRoot().hasPublishPermissions();
	}

	public void addUser(User user) {
		boot.get().userRoot().addUser(user);
	}

	public MeshOptions options() {
		return boot.get().userRoot().options();
	}

	public void removeUser(User user) {
		boot.get().userRoot().removeUser(user);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().userRoot().addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public void setCachedUuid(String uuid) {
		boot.get().userRoot().setCachedUuid(uuid);
	}

	public TraversalResult<? extends User> findAll() {
		return boot.get().userRoot().findAll();
	}

	public void setProperty(String name, Object value) {
		boot.get().userRoot().setProperty(name, value);
	}

	public User setPassword(User user, String password) {
		return boot.get().userRoot().setPassword(user, password);
	}

	public Class<?> getTypeResolution() {
		return boot.get().userRoot().getTypeResolution();
	}

	public Stream<? extends User> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return boot.get().userRoot().findAllStream(ac, permission);
	}

	public void setTypeResolution(Class<?> type) {
		boot.get().userRoot().setTypeResolution(type);
	}

	public PermissionInfo getPermissionInfo(User user, MeshVertex vertex) {
		return boot.get().userRoot().getPermissionInfo(user, vertex);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().userRoot().addFramedEdgeExplicit(label, inVertex, kind);
	}

	public Set<GraphPermission> getPermissions(User user, MeshVertex vertex) {
		return boot.get().userRoot().getPermissions(user, vertex);
	}

	public void removeTypeResolution() {
		boot.get().userRoot().removeTypeResolution();
	}

	public VertexTraversal<?, ?, ?> v() {
		return boot.get().userRoot().v();
	}

	public boolean hasPermission(User user, MeshVertex element, GraphPermission permission) {
		return boot.get().userRoot().hasPermission(user, element, permission);
	}

	public EdgeTraversal<?, ?, ?> e() {
		return boot.get().userRoot().e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return boot.get().userRoot().e(ids);
	}

	public boolean hasPermissionForId(User user, Object elementId, GraphPermission permission) {
		return boot.get().userRoot().hasPermissionForId(user, elementId, permission);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return boot.get().userRoot().addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return boot.get().userRoot().getGraphAttribute(key);
	}

	public TraversalResult<? extends User> findAllDynamic() {
		return boot.get().userRoot().findAllDynamic();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return boot.get().userRoot().in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return boot.get().userRoot().outE(labels);
	}

	public TransformablePage<? extends User> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().userRoot().findAll(ac, pagingInfo);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return boot.get().userRoot().inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().userRoot().linkOut(vertex, labels);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().userRoot().linkIn(vertex, labels);
	}

	public TransformablePage<? extends User> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<User> extraFilter) {
		return boot.get().userRoot().findAll(ac, pagingInfo, extraFilter);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().userRoot().unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().userRoot().unlinkIn(vertex, labels);
	}

	public TransformablePage<? extends User> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().userRoot().findAllNoPerm(ac, pagingInfo);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().userRoot().setLinkOut(vertex, labels);
	}

	public User findByName(String name) {
		return boot.get().userRoot().findByName(name);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return boot.get().userRoot().traversal();
	}

	public JsonObject toJson() {
		return boot.get().userRoot().toJson();
	}

	public User findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return boot.get().userRoot().findByName(ac, name, perm);
	}

	public <T> T reframe(Class<T> kind) {
		return boot.get().userRoot().reframe(kind);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return boot.get().userRoot().reframeExplicit(kind);
	}

	public User findByUuid(String uuid) {
		return boot.get().userRoot().findByUuid(uuid);
	}

	public User loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return boot.get().userRoot().loadObjectByUuid(ac, uuid, perm);
	}

	public User loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return boot.get().userRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	public User loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().userRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	public MeshVertex resolveToElement(Stack<String> stack) {
		return boot.get().userRoot().resolveToElement(stack);
	}

	public User create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().userRoot().create(ac, batch);
	}

	public User create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().userRoot().create(ac, batch, uuid);
	}

	public void addItem(User item) {
		boot.get().userRoot().addItem(item);
	}

	public void removeItem(User item) {
		boot.get().userRoot().removeItem(item);
	}

	public String getRootLabel() {
		return boot.get().userRoot().getRootLabel();
	}

	public Class<? extends User> getPersistanceClass() {
		return boot.get().userRoot().getPersistanceClass();
	}

	public long computeCount() {
		return boot.get().userRoot().computeCount();
	}

	@Override
	public void delete(User element, BulkActionContext bac) {
		boot.get().userRoot().delete(element, bac);
	}

	@Override
	public User addPermissionsOnRole(User user, HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode,
		GraphPermission... toGrant) {
		return boot.get().userRoot().addPermissionsOnRole(user, sourceNode, permission, targetNode, toGrant);
	}

	@Override
	public User addCRUDPermissionOnRole(User user, HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode) {
		return boot.get().userRoot().addCRUDPermissionOnRole(user, sourceNode, permission, targetNode);
	}

	@Override
	public boolean canReadNode(User user, InternalActionContext ac, Node node) {
		return boot.get().userRoot().canReadNode(user, ac, node);
	}

	@Override
	public String getAPIPath(User element, InternalActionContext ac) {
		return boot.get().userRoot().getAPIPath(element, ac);
	}

	@Override
	public String getETag(User element, InternalActionContext ac) {
		return boot.get().userRoot().getETag(element, ac);
	}

	@Override
	public boolean hasReadPermission(User user, NodeGraphFieldContainer container, String branchUuid, String requestedVersion) {
		return boot.get().userRoot().hasReadPermission(user, container, branchUuid, requestedVersion);
	}

	@Override
	public User inheritRolePermissions(User user, MeshVertex sourceNode, MeshVertex targetNode) {
		return boot.get().userRoot().inheritRolePermissions(user, sourceNode, targetNode);
	}

	@Override
	public UserResponse transformToRestSync(User element, InternalActionContext ac, int level, String... languageTags) {
		return boot.get().userRoot().transformToRestSync(element, ac, level, languageTags);
	}

	@Override
	public boolean update(User user, InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().userRoot().update(user, ac, batch);
	}

	@Override
	public boolean updateDry(User user, InternalActionContext ac) {
		return boot.get().userRoot().updateDry(user, ac);
	}

	@Override
	public String getSubETag(User user, InternalActionContext ac) {
		return boot.get().userRoot().getSubETag(user, ac);
	}

}
