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
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.error.MeshSchemaException;
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

public class SchemaDaoWrapperImpl implements SchemaDaoWrapper {

	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public SchemaDaoWrapperImpl(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

	public Object id() {
		return boot.get().schemaContainerRoot().id();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return boot.get().schemaContainerRoot().getRolePermissions(ac, roleUuid);
	}

	public void setUuid(String uuid) {
		boot.get().schemaContainerRoot().setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().schemaContainerRoot().setUniqueLinkOutTo(vertex, labels);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return boot.get().schemaContainerRoot().getRolesWithPerm(perm);
	}

	public String getUuid() {
		return boot.get().schemaContainerRoot().getUuid();
	}

	public SchemaContainer create(SchemaModel schema, User creator) throws MeshSchemaException {
		return boot.get().schemaContainerRoot().create(schema, creator);
	}

	public Vertex getVertex() {
		return boot.get().schemaContainerRoot().getVertex();
	}

	public String getElementVersion() {
		return boot.get().schemaContainerRoot().getElementVersion();
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().schemaContainerRoot().setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return boot.get().schemaContainerRoot().property(name);
	}

	public void delete(BulkActionContext bac) {
		boot.get().schemaContainerRoot().delete(bac);
	}

	public Vertex getElement() {
		return boot.get().schemaContainerRoot().getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().schemaContainerRoot().setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return boot.get().schemaContainerRoot().getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().schemaContainerRoot().addFramedEdge(label, inVertex, initializer);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().schemaContainerRoot().setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return boot.get().schemaContainerRoot().getPropertyKeys();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		boot.get().schemaContainerRoot().addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return boot.get().schemaContainerRoot().out(labels);
	}

	public void remove() {
		boot.get().schemaContainerRoot().remove();
	}

	public void delete() {
		boot.get().schemaContainerRoot().delete();
	}

	public SchemaContainer create(SchemaModel schema, User creator, String uuid) throws MeshSchemaException {
		return boot.get().schemaContainerRoot().create(schema, creator, uuid);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return boot.get().schemaContainerRoot().out(label, clazz);
	}

	public FramedGraph getGraph() {
		return boot.get().schemaContainerRoot().getGraph();
	}

	public <R> void property(String key, R value) {
		boot.get().schemaContainerRoot().property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		boot.get().schemaContainerRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return boot.get().schemaContainerRoot().outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return boot.get().schemaContainerRoot().getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return boot.get().schemaContainerRoot().in(label, clazz);
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().schemaContainerRoot().addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		boot.get().schemaContainerRoot().removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return boot.get().schemaContainerRoot().inE(label, clazz);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return boot.get().schemaContainerRoot().traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return boot.get().schemaContainerRoot().getProperty(name, type);
	}

	public Database db() {
		return boot.get().schemaContainerRoot().db();
	}

	public SchemaContainer create(SchemaModel schema, User creator, String uuid, boolean validate) throws MeshSchemaException {
		return boot.get().schemaContainerRoot().create(schema, creator, uuid, validate);
	}

	public Vertx vertx() {
		return boot.get().schemaContainerRoot().vertx();
	}

	public boolean hasPublishPermissions() {
		return boot.get().schemaContainerRoot().hasPublishPermissions();
	}

	public MeshOptions options() {
		return boot.get().schemaContainerRoot().options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().schemaContainerRoot().addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public void setCachedUuid(String uuid) {
		boot.get().schemaContainerRoot().setCachedUuid(uuid);
	}

	public TraversalResult<? extends SchemaContainer> findAll() {
		return boot.get().schemaContainerRoot().findAll();
	}

	public void setProperty(String name, Object value) {
		boot.get().schemaContainerRoot().setProperty(name, value);
	}

	public void addSchemaContainer(User user, SchemaContainer schemaContainer, EventQueueBatch batch) {
		boot.get().schemaContainerRoot().addSchemaContainer(user, schemaContainer, batch);
	}

	public Class<?> getTypeResolution() {
		return boot.get().schemaContainerRoot().getTypeResolution();
	}

	public Stream<? extends SchemaContainer> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return boot.get().schemaContainerRoot().findAllStream(ac, permission);
	}

	public void setTypeResolution(Class<?> type) {
		boot.get().schemaContainerRoot().setTypeResolution(type);
	}

	public void removeSchemaContainer(SchemaContainer schemaContainer, EventQueueBatch batch) {
		boot.get().schemaContainerRoot().removeSchemaContainer(schemaContainer, batch);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().schemaContainerRoot().addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		boot.get().schemaContainerRoot().removeTypeResolution();
	}

	public boolean contains(SchemaContainer schema) {
		return boot.get().schemaContainerRoot().contains(schema);
	}

	public VertexTraversal<?, ?, ?> v() {
		return boot.get().schemaContainerRoot().v();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return boot.get().schemaContainerRoot().e();
	}

	public SchemaContainerVersion fromReference(SchemaReference reference) {
		return boot.get().schemaContainerRoot().fromReference(reference);
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return boot.get().schemaContainerRoot().e(ids);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return boot.get().schemaContainerRoot().addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return boot.get().schemaContainerRoot().getGraphAttribute(key);
	}

	public Project getProject() {
		return boot.get().schemaContainerRoot().getProject();
	}

	public TraversalResult<? extends SchemaContainer> findAllDynamic() {
		return boot.get().schemaContainerRoot().findAllDynamic();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return boot.get().schemaContainerRoot().in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return boot.get().schemaContainerRoot().outE(labels);
	}

	public TransformablePage<? extends SchemaContainer> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().schemaContainerRoot().findAll(ac, pagingInfo);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return boot.get().schemaContainerRoot().inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().schemaContainerRoot().linkOut(vertex, labels);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().schemaContainerRoot().linkIn(vertex, labels);
	}

	public TransformablePage<? extends SchemaContainer> findAll(InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<SchemaContainer> extraFilter) {
		return boot.get().schemaContainerRoot().findAll(ac, pagingInfo, extraFilter);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().schemaContainerRoot().unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().schemaContainerRoot().unlinkIn(vertex, labels);
	}

	public TransformablePage<? extends SchemaContainer> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().schemaContainerRoot().findAllNoPerm(ac, pagingInfo);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().schemaContainerRoot().setLinkOut(vertex, labels);
	}

	public SchemaContainer findByName(String name) {
		return boot.get().schemaContainerRoot().findByName(name);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return boot.get().schemaContainerRoot().traversal();
	}

	public JsonObject toJson() {
		return boot.get().schemaContainerRoot().toJson();
	}

	public SchemaContainer findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return boot.get().schemaContainerRoot().findByName(ac, name, perm);
	}

	public <T> T reframe(Class<T> kind) {
		return boot.get().schemaContainerRoot().reframe(kind);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return boot.get().schemaContainerRoot().reframeExplicit(kind);
	}

	public SchemaContainer findByUuid(String uuid) {
		return boot.get().schemaContainerRoot().findByUuid(uuid);
	}

	public SchemaContainer loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return boot.get().schemaContainerRoot().loadObjectByUuid(ac, uuid, perm);
	}

	public SchemaContainer loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return boot.get().schemaContainerRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	public SchemaContainer loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().schemaContainerRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	public MeshVertex resolveToElement(Stack<String> stack) {
		return boot.get().schemaContainerRoot().resolveToElement(stack);
	}

	public SchemaContainer create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().schemaContainerRoot().create(ac, batch);
	}

	public SchemaContainer create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().schemaContainerRoot().create(ac, batch, uuid);
	}

	public void addItem(SchemaContainer item) {
		boot.get().schemaContainerRoot().addItem(item);
	}

	public void removeItem(SchemaContainer item) {
		boot.get().schemaContainerRoot().removeItem(item);
	}

	public String getRootLabel() {
		return boot.get().schemaContainerRoot().getRootLabel();
	}

	public Class<? extends SchemaContainer> getPersistanceClass() {
		return boot.get().schemaContainerRoot().getPersistanceClass();
	}

	public long computeCount() {
		return boot.get().schemaContainerRoot().computeCount();
	}

}
