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
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.impl.GroupWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.Assignment;
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
public class GroupDaoWrapperImpl implements GroupDaoWrapper {

	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public GroupDaoWrapperImpl(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

	public Object id() {
		return boot.get().groupRoot().id();
	}

	public Group create(String name, User user) {
		return boot.get().groupRoot().create(name, user);
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return boot.get().groupRoot().getRolePermissions(ac, roleUuid);
	}

	public void setUuid(String uuid) {
		boot.get().groupRoot().setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().groupRoot().setUniqueLinkOutTo(vertex, labels);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return boot.get().groupRoot().getRolesWithPerm(perm);
	}

	public String getUuid() {
		return boot.get().groupRoot().getUuid();
	}

	public Group create(String name, User user, String uuid) {
		return boot.get().groupRoot().create(name, user, uuid);
	}

	public Vertex getVertex() {
		return boot.get().groupRoot().getVertex();
	}

	public String getElementVersion() {
		return boot.get().groupRoot().getElementVersion();
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().groupRoot().setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return boot.get().groupRoot().property(name);
	}

	public void delete(BulkActionContext bac) {
		boot.get().groupRoot().delete(bac);
	}

	public Vertex getElement() {
		return boot.get().groupRoot().getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().groupRoot().setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return boot.get().groupRoot().getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().groupRoot().addFramedEdge(label, inVertex, initializer);
	}

	public void addGroup(Group group) {
		boot.get().groupRoot().addGroup(group);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().groupRoot().setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return boot.get().groupRoot().getPropertyKeys();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		boot.get().groupRoot().addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return boot.get().groupRoot().out(labels);
	}

	public void remove() {
		boot.get().groupRoot().remove();
	}

	public void removeGroup(Group group) {
		boot.get().groupRoot().removeGroup(group);
	}

	public void delete() {
		boot.get().groupRoot().delete();
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return boot.get().groupRoot().out(label, clazz);
	}

	public FramedGraph getGraph() {
		return boot.get().groupRoot().getGraph();
	}

	public <R> void property(String key, R value) {
		boot.get().groupRoot().property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		boot.get().groupRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return boot.get().groupRoot().outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return boot.get().groupRoot().getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return boot.get().groupRoot().in(label, clazz);
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().groupRoot().addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		boot.get().groupRoot().removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return boot.get().groupRoot().inE(label, clazz);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return boot.get().groupRoot().traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return boot.get().groupRoot().getProperty(name, type);
	}

	public Database db() {
		return boot.get().groupRoot().db();
	}

	public Vertx vertx() {
		return boot.get().groupRoot().vertx();
	}

	public boolean hasPublishPermissions() {
		return boot.get().groupRoot().hasPublishPermissions();
	}

	public MeshOptions options() {
		return boot.get().groupRoot().options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().groupRoot().addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public void setCachedUuid(String uuid) {
		boot.get().groupRoot().setCachedUuid(uuid);
	}

	public TraversalResult<? extends Group> findAll() {
		return boot.get().groupRoot().findAll();
	}

	public void setProperty(String name, Object value) {
		boot.get().groupRoot().setProperty(name, value);
	}

	public Class<?> getTypeResolution() {
		return boot.get().groupRoot().getTypeResolution();
	}

	public Stream<? extends Group> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return boot.get().groupRoot().findAllStream(ac, permission);
	}

	public void setTypeResolution(Class<?> type) {
		boot.get().groupRoot().setTypeResolution(type);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().groupRoot().addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		boot.get().groupRoot().removeTypeResolution();
	}

	public VertexTraversal<?, ?, ?> v() {
		return boot.get().groupRoot().v();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return boot.get().groupRoot().e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return boot.get().groupRoot().e(ids);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return boot.get().groupRoot().addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return boot.get().groupRoot().getGraphAttribute(key);
	}

	public TraversalResult<? extends Group> findAllDynamic() {
		return boot.get().groupRoot().findAllDynamic();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return boot.get().groupRoot().in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return boot.get().groupRoot().outE(labels);
	}

	public TransformablePage<? extends Group> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().groupRoot().findAll(ac, pagingInfo);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return boot.get().groupRoot().inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().groupRoot().linkOut(vertex, labels);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().groupRoot().linkIn(vertex, labels);
	}

	public TransformablePage<? extends Group> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Group> extraFilter) {
		return boot.get().groupRoot().findAll(ac, pagingInfo, extraFilter);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().groupRoot().unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().groupRoot().unlinkIn(vertex, labels);
	}

	public TransformablePage<? extends Group> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().groupRoot().findAllNoPerm(ac, pagingInfo);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().groupRoot().setLinkOut(vertex, labels);
	}

	public Group findByName(String name) {
		return boot.get().groupRoot().findByName(name);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return boot.get().groupRoot().traversal();
	}

	public JsonObject toJson() {
		return boot.get().groupRoot().toJson();
	}

	public Group findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return boot.get().groupRoot().findByName(ac, name, perm);
	}

	public <T> T reframe(Class<T> kind) {
		return boot.get().groupRoot().reframe(kind);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return boot.get().groupRoot().reframeExplicit(kind);
	}

	public Group findByUuid(String uuid) {
		return boot.get().groupRoot().findByUuid(uuid);
	}

	public Group loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return boot.get().groupRoot().loadObjectByUuid(ac, uuid, perm);
	}

	public Group loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return boot.get().groupRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	public Group loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().groupRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	public MeshVertex resolveToElement(Stack<String> stack) {
		return boot.get().groupRoot().resolveToElement(stack);
	}

	public Group create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().groupRoot().create(ac, batch);
	}

	public Group create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().groupRoot().create(ac, batch, uuid);
	}

	public void addItem(Group item) {
		boot.get().groupRoot().addItem(item);
	}

	public void removeItem(Group item) {
		boot.get().groupRoot().removeItem(item);
	}

	public String getRootLabel() {
		return boot.get().groupRoot().getRootLabel();
	}

	public Class<? extends Group> getPersistanceClass() {
		return boot.get().groupRoot().getPersistanceClass();
	}

	public long computeCount() {
		return boot.get().groupRoot().computeCount();
	}

	@Override
	public void addUser(Group group, User user) {
		boot.get().groupRoot().addUser(group, user);
	}

	@Override
	public void removeUser(Group group, User user) {
		boot.get().groupRoot().removeUser(group, user);
	}

	@Override
	public void addRole(Group group, Role role) {
		boot.get().groupRoot().addRole(group, role);
	}

	@Override
	public void removeRole(Group group, Role role) {
		boot.get().groupRoot().removeRole(group, role);
	}

	@Override
	public TraversalResult<? extends User> getUsers(Group group) {
		return boot.get().groupRoot().getUsers(group);
	}

	@Override
	public TraversalResult<? extends Role> getRoles(Group group) {
		return boot.get().groupRoot().getRoles(group);
	}

	@Override
	public boolean hasUser(Group group, User user) {
		return boot.get().groupRoot().hasUser(group, user);
	}

	@Override
	public boolean hasRole(Group group, Role role) {
		return boot.get().groupRoot().hasRole(group, role);
	}

	@Override
	public TransformablePage<? extends Role> getRoles(Group group, User user, PagingParameters pagingInfo) {
		return boot.get().groupRoot().getRoles(group, user, pagingInfo);
	}

	@Override
	public TransformablePage<? extends User> getVisibleUsers(Group group, MeshAuthUser requestUser, PagingParameters pagingInfo) {
		return boot.get().groupRoot().getVisibleUsers(group, requestUser, pagingInfo);
	}

	@Override
	public GroupUserAssignModel createUserAssignmentEvent(Group group, User user, Assignment assignment) {
		return boot.get().groupRoot().createUserAssignmentEvent(group, user, assignment);
	}

	@Override
	public GroupRoleAssignModel createRoleAssignmentEvent(Group group, Role role, Assignment assignment) {
		return boot.get().groupRoot().createRoleAssignmentEvent(group, role, assignment);
	}

	@Override
	public PermissionInfo getRolePermissions(MeshVertex vertex, InternalActionContext ac, String roleUuid) {
		return boot.get().groupRoot().getRolePermissions(vertex, ac, roleUuid);
	}

	@Override
	public TraversalResult<? extends Role> getRolesWithPerm(MeshVertex vertex, GraphPermission perm) {
		return boot.get().groupRoot().getRolesWithPerm(vertex, perm);
	}

	@Override
	public void delete(Group element, BulkActionContext bac) {
		boot.get().groupRoot().delete(element, bac);
	}

	@Override
	public String getAPIPath(Group element, InternalActionContext ac) {
		return boot.get().groupRoot().getAPIPath(element, ac);
	}

	@Override
	public Single<GroupResponse> transformToRest(Group element, InternalActionContext ac, int level, String... languageTags) {
		return boot.get().groupRoot().transformToRest(element, ac, level, languageTags);
	}

	@Override
	public GroupResponse transformToRestSync(Group element, InternalActionContext ac, int level, String... languageTags) {
		return boot.get().groupRoot().transformToRestSync(element, ac, level, languageTags);
	}

	@Override
	public boolean update(Group element, InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().groupRoot().update(element, ac, batch);
	}

	@Override
	public String getETag(Group element, InternalActionContext ac) {
		return boot.get().groupRoot().getETag(element, ac);
	}
}
