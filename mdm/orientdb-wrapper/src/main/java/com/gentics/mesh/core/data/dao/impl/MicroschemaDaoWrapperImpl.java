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
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
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

public class MicroschemaDaoWrapperImpl extends AbstractDaoWrapper implements MicroschemaDaoWrapper {

	@Inject
	public MicroschemaDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions) {
		super(boot, permissions);
	}

	public Object id() {
		return boot.get().microschemaContainerRoot().id();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return boot.get().microschemaContainerRoot().getRolePermissions(ac, roleUuid);
	}

	public void setUuid(String uuid) {
		boot.get().microschemaContainerRoot().setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().microschemaContainerRoot().setUniqueLinkOutTo(vertex, labels);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return boot.get().microschemaContainerRoot().getRolesWithPerm(perm);
	}

	public String getUuid() {
		return boot.get().microschemaContainerRoot().getUuid();
	}

	public void addMicroschema(User user, MicroschemaContainer container, EventQueueBatch batch) {
		boot.get().microschemaContainerRoot().addMicroschema(user, container, batch);
	}

	public Vertex getVertex() {
		return boot.get().microschemaContainerRoot().getVertex();
	}

	public String getElementVersion() {
		return boot.get().microschemaContainerRoot().getElementVersion();
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().microschemaContainerRoot().setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return boot.get().microschemaContainerRoot().property(name);
	}

	public void delete(BulkActionContext bac) {
		boot.get().microschemaContainerRoot().delete(bac);
	}

	public Vertex getElement() {
		return boot.get().microschemaContainerRoot().getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().microschemaContainerRoot().setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return boot.get().microschemaContainerRoot().getId();
	}

	public void removeMicroschema(MicroschemaContainer container, EventQueueBatch batch) {
		boot.get().microschemaContainerRoot().removeMicroschema(container, batch);
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().microschemaContainerRoot().addFramedEdge(label, inVertex, initializer);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().microschemaContainerRoot().setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return boot.get().microschemaContainerRoot().getPropertyKeys();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		boot.get().microschemaContainerRoot().addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return boot.get().microschemaContainerRoot().out(labels);
	}

	public void remove() {
		boot.get().microschemaContainerRoot().remove();
	}

	public MicroschemaContainer create(MicroschemaModel microschema, User user, EventQueueBatch batch) {
		return boot.get().microschemaContainerRoot().create(microschema, user, batch);
	}

	public void delete() {
		boot.get().microschemaContainerRoot().delete();
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return boot.get().microschemaContainerRoot().out(label, clazz);
	}

	public FramedGraph getGraph() {
		return boot.get().microschemaContainerRoot().getGraph();
	}

	public <R> void property(String key, R value) {
		boot.get().microschemaContainerRoot().property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		boot.get().microschemaContainerRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return boot.get().microschemaContainerRoot().outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return boot.get().microschemaContainerRoot().getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return boot.get().microschemaContainerRoot().in(label, clazz);
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().microschemaContainerRoot().addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		boot.get().microschemaContainerRoot().removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return boot.get().microschemaContainerRoot().inE(label, clazz);
	}

	public MicroschemaContainer create(MicroschemaModel microschema, User user, String uuid, EventQueueBatch batch) {
		return boot.get().microschemaContainerRoot().create(microschema, user, uuid, batch);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return boot.get().microschemaContainerRoot().traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return boot.get().microschemaContainerRoot().getProperty(name, type);
	}

	public Database db() {
		return boot.get().microschemaContainerRoot().db();
	}

	public Vertx vertx() {
		return boot.get().microschemaContainerRoot().vertx();
	}

	public boolean hasPublishPermissions() {
		return boot.get().microschemaContainerRoot().hasPublishPermissions();
	}

	public MeshOptions options() {
		return boot.get().microschemaContainerRoot().options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().microschemaContainerRoot().addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public boolean contains(MicroschemaContainer microschema) {
		return boot.get().microschemaContainerRoot().contains(microschema);
	}

	public void setCachedUuid(String uuid) {
		boot.get().microschemaContainerRoot().setCachedUuid(uuid);
	}

	public TraversalResult<? extends MicroschemaContainer> findAll() {
		return boot.get().microschemaContainerRoot().findAll();
	}

	public void setProperty(String name, Object value) {
		boot.get().microschemaContainerRoot().setProperty(name, value);
	}

	public MicroschemaContainerVersion fromReference(MicroschemaReference reference) {
		return boot.get().microschemaContainerRoot().fromReference(reference);
	}

	public Class<?> getTypeResolution() {
		return boot.get().microschemaContainerRoot().getTypeResolution();
	}

	public Stream<? extends MicroschemaContainer> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return boot.get().microschemaContainerRoot().findAllStream(ac, permission);
	}

	public MicroschemaContainerVersion fromReference(MicroschemaReference reference, Branch branch) {
		return boot.get().microschemaContainerRoot().fromReference(reference, branch);
	}

	public void setTypeResolution(Class<?> type) {
		boot.get().microschemaContainerRoot().setTypeResolution(type);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().microschemaContainerRoot().addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		boot.get().microschemaContainerRoot().removeTypeResolution();
	}

	public VertexTraversal<?, ?, ?> v() {
		return boot.get().microschemaContainerRoot().v();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return boot.get().microschemaContainerRoot().e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return boot.get().microschemaContainerRoot().e(ids);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return boot.get().microschemaContainerRoot().addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return boot.get().microschemaContainerRoot().getGraphAttribute(key);
	}

	public TraversalResult<? extends MicroschemaContainer> findAllDynamic() {
		return boot.get().microschemaContainerRoot().findAllDynamic();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return boot.get().microschemaContainerRoot().in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return boot.get().microschemaContainerRoot().outE(labels);
	}

	public TransformablePage<? extends MicroschemaContainer> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().microschemaContainerRoot().findAll(ac, pagingInfo);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return boot.get().microschemaContainerRoot().inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().microschemaContainerRoot().linkOut(vertex, labels);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().microschemaContainerRoot().linkIn(vertex, labels);
	}

	public TransformablePage<? extends MicroschemaContainer> findAll(InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<MicroschemaContainer> extraFilter) {
		return boot.get().microschemaContainerRoot().findAll(ac, pagingInfo, extraFilter);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().microschemaContainerRoot().unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().microschemaContainerRoot().unlinkIn(vertex, labels);
	}

	public TransformablePage<? extends MicroschemaContainer> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().microschemaContainerRoot().findAllNoPerm(ac, pagingInfo);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().microschemaContainerRoot().setLinkOut(vertex, labels);
	}

	public MicroschemaContainer findByName(String name) {
		return boot.get().microschemaContainerRoot().findByName(name);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return boot.get().microschemaContainerRoot().traversal();
	}

	public JsonObject toJson() {
		return boot.get().microschemaContainerRoot().toJson();
	}

	public MicroschemaContainer findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return boot.get().microschemaContainerRoot().findByName(ac, name, perm);
	}

	public <T> T reframe(Class<T> kind) {
		return boot.get().microschemaContainerRoot().reframe(kind);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return boot.get().microschemaContainerRoot().reframeExplicit(kind);
	}

	public MicroschemaContainer findByUuid(String uuid) {
		return boot.get().microschemaContainerRoot().findByUuid(uuid);
	}

	public MicroschemaContainer loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return boot.get().microschemaContainerRoot().loadObjectByUuid(ac, uuid, perm);
	}

	public MicroschemaContainer loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return boot.get().microschemaContainerRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	public MicroschemaContainer loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().microschemaContainerRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	public MeshVertex resolveToElement(Stack<String> stack) {
		return boot.get().microschemaContainerRoot().resolveToElement(stack);
	}

	public MicroschemaContainer create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().microschemaContainerRoot().create(ac, batch);
	}

	public MicroschemaContainer create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().microschemaContainerRoot().create(ac, batch, uuid);
	}

	public void addItem(MicroschemaContainer item) {
		boot.get().microschemaContainerRoot().addItem(item);
	}

	public void removeItem(MicroschemaContainer item) {
		boot.get().microschemaContainerRoot().removeItem(item);
	}

	public String getRootLabel() {
		return boot.get().microschemaContainerRoot().getRootLabel();
	}

	public Class<? extends MicroschemaContainer> getPersistanceClass() {
		return boot.get().microschemaContainerRoot().getPersistanceClass();
	}

	public long computeCount() {
		return boot.get().microschemaContainerRoot().computeCount();
	}

	@Override
	public boolean update(MicroschemaContainer element, InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().microschemaContainerRoot().update(element, ac, batch);
	}

}
