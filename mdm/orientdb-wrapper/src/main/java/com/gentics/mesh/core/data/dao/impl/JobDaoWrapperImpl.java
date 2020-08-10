package com.gentics.mesh.core.data.dao.impl;

import java.time.ZonedDateTime;
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
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.common.PermissionInfo;
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
import io.reactivex.Completable;
import io.vertx.core.Vertx;

@Singleton
public class JobDaoWrapperImpl extends AbstractDaoWrapper implements JobDaoWrapper {

	@Inject
	public JobDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions) {
		super(boot, permissions);
	}

	public Object id() {
		return boot.get().jobRoot().id();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return boot.get().jobRoot().getRolePermissions(ac, roleUuid);
	}

	public void setUuid(String uuid) {
		boot.get().jobRoot().setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().jobRoot().setUniqueLinkOutTo(vertex, labels);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return boot.get().jobRoot().getRolesWithPerm(perm);
	}

	public Job enqueueSchemaMigration(User creator, Branch branch, SchemaVersion fromVersion, SchemaVersion toVersion) {
		return boot.get().jobRoot().enqueueSchemaMigration(creator, branch, fromVersion, toVersion);
	}

	public String getUuid() {
		return boot.get().jobRoot().getUuid();
	}

	public Vertex getVertex() {
		return boot.get().jobRoot().getVertex();
	}

	public String getElementVersion() {
		return boot.get().jobRoot().getElementVersion();
	}

	public Job enqueueBranchMigration(User creator, Branch branch, SchemaVersion fromVersion, SchemaVersion toVersion) {
		return boot.get().jobRoot().enqueueBranchMigration(creator, branch, fromVersion, toVersion);
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().jobRoot().setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return boot.get().jobRoot().property(name);
	}

	public void delete(BulkActionContext bac) {
		boot.get().jobRoot().delete(bac);
	}

	public Vertex getElement() {
		return boot.get().jobRoot().getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().jobRoot().setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return boot.get().jobRoot().getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().jobRoot().addFramedEdge(label, inVertex, initializer);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().jobRoot().setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return boot.get().jobRoot().getPropertyKeys();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		boot.get().jobRoot().addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return boot.get().jobRoot().out(labels);
	}

	public void remove() {
		boot.get().jobRoot().remove();
	}

	public Job enqueueMicroschemaMigration(User creator, Branch branch, MicroschemaVersion fromVersion,
		MicroschemaVersion toVersion) {
		return boot.get().jobRoot().enqueueMicroschemaMigration(creator, branch, fromVersion, toVersion);
	}

	public void delete() {
		boot.get().jobRoot().delete();
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return boot.get().jobRoot().out(label, clazz);
	}

	public FramedGraph getGraph() {
		return boot.get().jobRoot().getGraph();
	}

	public <R> void property(String key, R value) {
		boot.get().jobRoot().property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		boot.get().jobRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return boot.get().jobRoot().outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return boot.get().jobRoot().getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return boot.get().jobRoot().in(label, clazz);
	}

	public Job enqueueBranchMigration(User creator, Branch branch) {
		return boot.get().jobRoot().enqueueBranchMigration(creator, branch);
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().jobRoot().addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		boot.get().jobRoot().removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return boot.get().jobRoot().inE(label, clazz);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return boot.get().jobRoot().traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return boot.get().jobRoot().getProperty(name, type);
	}

	public Database db() {
		return boot.get().jobRoot().db();
	}

	public Job enqueueVersionPurge(User user, Project project, ZonedDateTime before) {
		return boot.get().jobRoot().enqueueVersionPurge(user, project, before);
	}

	public Vertx vertx() {
		return boot.get().jobRoot().vertx();
	}

	public boolean hasPublishPermissions() {
		return boot.get().jobRoot().hasPublishPermissions();
	}

	public MeshOptions options() {
		return boot.get().jobRoot().options();
	}

	public Job enqueueVersionPurge(User user, Project project) {
		return boot.get().jobRoot().enqueueVersionPurge(user, project);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().jobRoot().addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public void setCachedUuid(String uuid) {
		boot.get().jobRoot().setCachedUuid(uuid);
	}

	public TraversalResult<? extends Job> findAll() {
		return boot.get().jobRoot().findAll();
	}

	public void setProperty(String name, Object value) {
		boot.get().jobRoot().setProperty(name, value);
	}

	public Completable process() {
		return boot.get().jobRoot().process();
	}

	public void purgeFailed() {
		boot.get().jobRoot().purgeFailed();
	}

	public void clear() {
		boot.get().jobRoot().clear();
	}

	public Class<?> getTypeResolution() {
		return boot.get().jobRoot().getTypeResolution();
	}

	public Stream<? extends Job> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return boot.get().jobRoot().findAllStream(ac, permission);
	}

	public void setTypeResolution(Class<?> type) {
		boot.get().jobRoot().setTypeResolution(type);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().jobRoot().addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		boot.get().jobRoot().removeTypeResolution();
	}

	public VertexTraversal<?, ?, ?> v() {
		return boot.get().jobRoot().v();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return boot.get().jobRoot().e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return boot.get().jobRoot().e(ids);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return boot.get().jobRoot().addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return boot.get().jobRoot().getGraphAttribute(key);
	}

	public TraversalResult<? extends Job> findAllDynamic() {
		return boot.get().jobRoot().findAllDynamic();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return boot.get().jobRoot().in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return boot.get().jobRoot().outE(labels);
	}

	public TransformablePage<? extends Job> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().jobRoot().findAll(ac, pagingInfo);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return boot.get().jobRoot().inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().jobRoot().linkOut(vertex, labels);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().jobRoot().linkIn(vertex, labels);
	}

	public TransformablePage<? extends Job> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Job> extraFilter) {
		return boot.get().jobRoot().findAll(ac, pagingInfo, extraFilter);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().jobRoot().unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().jobRoot().unlinkIn(vertex, labels);
	}

	public TransformablePage<? extends Job> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().jobRoot().findAllNoPerm(ac, pagingInfo);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().jobRoot().setLinkOut(vertex, labels);
	}

	public Job findByName(String name) {
		return boot.get().jobRoot().findByName(name);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return boot.get().jobRoot().traversal();
	}

	public JsonObject toJson() {
		return boot.get().jobRoot().toJson();
	}

	public Job findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return boot.get().jobRoot().findByName(ac, name, perm);
	}

	public <T> T reframe(Class<T> kind) {
		return boot.get().jobRoot().reframe(kind);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return boot.get().jobRoot().reframeExplicit(kind);
	}

	public Job findByUuid(String uuid) {
		return boot.get().jobRoot().findByUuid(uuid);
	}

	public Job loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return boot.get().jobRoot().loadObjectByUuid(ac, uuid, perm);
	}

	public Job loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return boot.get().jobRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	public Job loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().jobRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	public MeshVertex resolveToElement(Stack<String> stack) {
		return boot.get().jobRoot().resolveToElement(stack);
	}

	public Job create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().jobRoot().create(ac, batch);
	}

	public Job create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().jobRoot().create(ac, batch, uuid);
	}

	public void addItem(Job item) {
		boot.get().jobRoot().addItem(item);
	}

	public void removeItem(Job item) {
		boot.get().jobRoot().removeItem(item);
	}

	public String getRootLabel() {
		return boot.get().jobRoot().getRootLabel();
	}

	public Class<? extends Job> getPersistanceClass() {
		return boot.get().jobRoot().getPersistanceClass();
	}

	public long computeCount() {
		return boot.get().jobRoot().computeCount();
	}

}
