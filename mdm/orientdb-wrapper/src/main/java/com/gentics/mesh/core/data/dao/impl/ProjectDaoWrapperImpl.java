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
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.project.ProjectResponse;
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

// Use ProjectDao instead of ProjectRoot once ready
@Singleton
public class ProjectDaoWrapperImpl implements ProjectDaoWrapper {

	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public ProjectDaoWrapperImpl(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

	public Object id() {
		return boot.get().projectRoot().id();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return boot.get().projectRoot().getRolePermissions(ac, roleUuid);
	}

	public void setUuid(String uuid) {
		boot.get().projectRoot().setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().projectRoot().setUniqueLinkOutTo(vertex, labels);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return boot.get().projectRoot().getRolesWithPerm(perm);
	}

	public String getUuid() {
		return boot.get().projectRoot().getUuid();
	}

	public Vertex getVertex() {
		return boot.get().projectRoot().getVertex();
	}

	public String getElementVersion() {
		return boot.get().projectRoot().getElementVersion();
	}

	public Project create(String projectName, String hostname, Boolean ssl, String pathPrefix, User creator,
		SchemaContainerVersion schemaContainerVersion, EventQueueBatch batch) {
		return boot.get().projectRoot().create(projectName, hostname, ssl, pathPrefix, creator, schemaContainerVersion, batch);
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().projectRoot().setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return boot.get().projectRoot().property(name);
	}

	public void delete(BulkActionContext bac) {
		boot.get().projectRoot().delete(bac);
	}

	public Vertex getElement() {
		return boot.get().projectRoot().getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().projectRoot().setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return boot.get().projectRoot().getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().projectRoot().addFramedEdge(label, inVertex, initializer);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().projectRoot().setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return boot.get().projectRoot().getPropertyKeys();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		boot.get().projectRoot().addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return boot.get().projectRoot().out(labels);
	}

	public void remove() {
		boot.get().projectRoot().remove();
	}

	public void delete() {
		boot.get().projectRoot().delete();
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return boot.get().projectRoot().out(label, clazz);
	}

	public FramedGraph getGraph() {
		return boot.get().projectRoot().getGraph();
	}

	public <R> void property(String key, R value) {
		boot.get().projectRoot().property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		boot.get().projectRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return boot.get().projectRoot().outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return boot.get().projectRoot().getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return boot.get().projectRoot().in(label, clazz);
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().projectRoot().addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		boot.get().projectRoot().removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return boot.get().projectRoot().inE(label, clazz);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return boot.get().projectRoot().traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return boot.get().projectRoot().getProperty(name, type);
	}

	public Database db() {
		return boot.get().projectRoot().db();
	}

	public Vertx vertx() {
		return boot.get().projectRoot().vertx();
	}

	public boolean hasPublishPermissions() {
		return boot.get().projectRoot().hasPublishPermissions();
	}

	public MeshOptions options() {
		return boot.get().projectRoot().options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().projectRoot().addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public void setCachedUuid(String uuid) {
		boot.get().projectRoot().setCachedUuid(uuid);
	}

	public TraversalResult<? extends Project> findAll() {
		return boot.get().projectRoot().findAll();
	}

	public Project create(String projectName, String hostname, Boolean ssl, String pathPrefix, User creator,
		SchemaContainerVersion schemaContainerVersion, String uuid, EventQueueBatch batch) {
		return boot.get().projectRoot().create(projectName, hostname, ssl, pathPrefix, creator, schemaContainerVersion, uuid, batch);
	}

	public void setProperty(String name, Object value) {
		boot.get().projectRoot().setProperty(name, value);
	}

	public Class<?> getTypeResolution() {
		return boot.get().projectRoot().getTypeResolution();
	}

	public Stream<? extends Project> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return boot.get().projectRoot().findAllStream(ac, permission);
	}

	public void setTypeResolution(Class<?> type) {
		boot.get().projectRoot().setTypeResolution(type);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().projectRoot().addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		boot.get().projectRoot().removeTypeResolution();
	}

	public VertexTraversal<?, ?, ?> v() {
		return boot.get().projectRoot().v();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return boot.get().projectRoot().e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return boot.get().projectRoot().e(ids);
	}

	public void removeProject(Project project) {
		boot.get().projectRoot().removeProject(project);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return boot.get().projectRoot().addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return boot.get().projectRoot().getGraphAttribute(key);
	}

	public void addProject(Project project) {
		boot.get().projectRoot().addProject(project);
	}

	public TraversalResult<? extends Project> findAllDynamic() {
		return boot.get().projectRoot().findAllDynamic();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return boot.get().projectRoot().in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return boot.get().projectRoot().outE(labels);
	}

	public TransformablePage<? extends Project> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().projectRoot().findAll(ac, pagingInfo);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return boot.get().projectRoot().inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().projectRoot().linkOut(vertex, labels);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().projectRoot().linkIn(vertex, labels);
	}

	public TransformablePage<? extends Project> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Project> extraFilter) {
		return boot.get().projectRoot().findAll(ac, pagingInfo, extraFilter);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().projectRoot().unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().projectRoot().unlinkIn(vertex, labels);
	}

	public TransformablePage<? extends Project> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().projectRoot().findAllNoPerm(ac, pagingInfo);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().projectRoot().setLinkOut(vertex, labels);
	}

	public Project findByName(String name) {
		return boot.get().projectRoot().findByName(name);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return boot.get().projectRoot().traversal();
	}

	public JsonObject toJson() {
		return boot.get().projectRoot().toJson();
	}

	public Project findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return boot.get().projectRoot().findByName(ac, name, perm);
	}

	public <T> T reframe(Class<T> kind) {
		return boot.get().projectRoot().reframe(kind);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return boot.get().projectRoot().reframeExplicit(kind);
	}

	public Project findByUuid(String uuid) {
		return boot.get().projectRoot().findByUuid(uuid);
	}

	public Project loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return boot.get().projectRoot().loadObjectByUuid(ac, uuid, perm);
	}

	public Project loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return boot.get().projectRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	public Project loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().projectRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	public MeshVertex resolveToElement(Stack<String> stack) {
		return boot.get().projectRoot().resolveToElement(stack);
	}

	public Project create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().projectRoot().create(ac, batch);
	}

	public Project create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().projectRoot().create(ac, batch, uuid);
	}

	public void addItem(Project item) {
		boot.get().projectRoot().addItem(item);
	}

	public void removeItem(Project item) {
		boot.get().projectRoot().removeItem(item);
	}

	public String getRootLabel() {
		return boot.get().projectRoot().getRootLabel();
	}

	public Class<? extends Project> getPersistanceClass() {
		return boot.get().projectRoot().getPersistanceClass();
	}

	public long computeCount() {
		return boot.get().projectRoot().computeCount();
	}

	public String getAPIPath(Project element, InternalActionContext ac) {
		return boot.get().projectRoot().getAPIPath(element, ac);
	}

	public String getETag(Project element, InternalActionContext ac) {
		return boot.get().projectRoot().getAPIPath(element, ac);
	}

	public ProjectResponse transformToRestSync(Project element, InternalActionContext ac, int level, String[] languageTags) {
		return boot.get().projectRoot().transformToRestSync(element, ac, level, languageTags);
	}

	@Override
	public String getSubETag(Project project, InternalActionContext ac) {
		return boot.get().projectRoot().getSubETag(project, ac);
	}

	@Override
	public void delete(Project element, BulkActionContext bac) {
		boot.get().projectRoot().delete(element, bac);
	}

}
