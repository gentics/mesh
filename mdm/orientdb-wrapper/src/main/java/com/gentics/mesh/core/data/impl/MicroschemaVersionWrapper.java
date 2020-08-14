package com.gentics.mesh.core.data.impl;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.HibMicroschemaVersion;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerComparator;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.traversal.TraversalResult;
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

public class MicroschemaVersionWrapper implements MicroschemaVersion, HibMicroschemaVersion {

	private final MicroschemaVersion delegate;

	public MicroschemaVersionWrapper(MicroschemaVersion delegate) {
		this.delegate = delegate;
	}

	public static MicroschemaVersionWrapper wrap(MicroschemaVersion version) {
		if (version == null) {
			return null;
		} else {
			return new MicroschemaVersionWrapper(version);
		}
	}

	public Object id() {
		return delegate.id();
	}

	public MicroschemaReference transformToReference() {
		return delegate.transformToReference();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return delegate.getRolePermissions(ac, roleUuid);
	}

	public void setUuid(String uuid) {
		delegate.setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		delegate.setUniqueLinkOutTo(vertex, labels);
	}

	public String getName() {
		return delegate.getName();
	}

	public String getAPIPath(InternalActionContext ac) {
		return delegate.getAPIPath(ac);
	}

	public TraversalResult<? extends HibRole> getRolesWithPerm(InternalPermission perm) {
		return delegate.getRolesWithPerm(perm);
	}

	public String getUuid() {
		return delegate.getUuid();
	}

	public void setName(String name) {
		delegate.setName(name);
	}

	public Single<MicroschemaResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return delegate.transformToRest(ac, level, languageTags);
	}

	public Vertex getVertex() {
		return delegate.getVertex();
	}

	public String getElementVersion() {
		return delegate.getElementVersion();
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

	public MicroschemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		return delegate.transformToRestSync(ac, level, languageTags);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return delegate.out(label, clazz);
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

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return delegate.addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		delegate.removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return delegate.inE(label, clazz);
	}

	public TypeInfo getTypeInfo() {
		return delegate.getTypeInfo();
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return delegate.traverse(traverser);
	}

	public TraversalResult<? extends NodeGraphFieldContainer> getDraftFieldContainers(String branchUuid) {
		return delegate.getDraftFieldContainers(branchUuid);
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

	public String getVersion() {
		return delegate.getVersion();
	}

	public String getETag(InternalActionContext ac) {
		return delegate.getETag(ac);
	}

	public boolean hasPublishPermissions() {
		return delegate.hasPublishPermissions();
	}

	public MeshOptions options() {
		return delegate.options();
	}

	public void setVersion(String version) {
		delegate.setVersion(version);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return delegate.addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public MicroschemaVersionModel getSchema() {
		return delegate.getSchema();
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

	public void setSchema(MicroschemaVersionModel schema) {
		delegate.setSchema(schema);
	}

	public TraversalResult<? extends Micronode> findMicronodes() {
		return delegate.findMicronodes();
	}

	public MeshElementEventModel onUpdated() {
		return delegate.onUpdated();
	}

	public SchemaChange<?> getPreviousChange() {
		return delegate.getPreviousChange();
	}

	public MeshElementEventModel onDeleted() {
		return delegate.onDeleted();
	}

	public Class<?> getTypeResolution() {
		return delegate.getTypeResolution();
	}

	public SchemaChange<?> getNextChange() {
		return delegate.getNextChange();
	}

	public PermissionChangedEventModelImpl onPermissionChanged(Role role) {
		return delegate.onPermissionChanged(role);
	}

	public void setTypeResolution(Class<?> type) {
		delegate.setTypeResolution(type);
	}

	public Stream<SchemaChange<FieldSchemaContainer>> getChanges() {
		return delegate.getChanges();
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

	public void setNextChange(SchemaChange<?> change) {
		delegate.setNextChange(change);
	}

	public EdgeTraversal<?, ?, ?> e() {
		return delegate.e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return delegate.e(ids);
	}

	public void setPreviousChange(SchemaChange<?> change) {
		delegate.setPreviousChange(change);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return delegate.addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return delegate.getGraphAttribute(key);
	}

	public MicroschemaVersion getNextVersion() {
		return wrap(delegate.getNextVersion());
	}

	public void setNextVersion(MicroschemaVersion container) {
		delegate.setNextVersion(container);
	}

	public MicroschemaVersion getPreviousVersion() {
		return wrap(delegate.getPreviousVersion());
	}

	public Stream<MicroschemaVersion> getPreviousVersions() {
		return delegate.getPreviousVersions();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return delegate.in(labels);
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

	public void setPreviousVersion(MicroschemaVersion container) {
		delegate.setPreviousVersion(container);
	}

	public SchemaChangesListModel diff(InternalActionContext ac, FieldSchemaContainerComparator<?> comparator, FieldSchemaContainer restModel) {
		return delegate.diff(ac, comparator, restModel);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.linkIn(vertex, labels);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.unlinkOut(vertex, labels);
	}

	public MicroschemaVersion applyChanges(InternalActionContext ac, EventQueueBatch batch) {
		return wrap(delegate.applyChanges(ac, batch));
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.unlinkIn(vertex, labels);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.setLinkOut(vertex, labels);
	}

	public MicroschemaVersionWrapper applyChanges(InternalActionContext ac, SchemaChangesListModel listOfChanges, EventQueueBatch batch) {
		return wrap(delegate.applyChanges(ac, listOfChanges, batch));
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return delegate.traversal();
	}

	public MicroschemaWrapper getSchemaContainer() {
		return MicroschemaWrapper.wrap(delegate.getSchemaContainer());
	}

	public JsonObject toJson() {
		return delegate.toJson();
	}

	public void setSchemaContainer(Microschema container) {
		delegate.setSchemaContainer(container);
	}

	public <T> T reframe(Class<T> kind) {
		return delegate.reframe(kind);
	}

	public TraversalResult<? extends Branch> getBranches() {
		return delegate.getBranches();
	}

	public String getJson() {
		return delegate.getJson();
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return delegate.reframeExplicit(kind);
	}

	public void setJson(String json) {
		delegate.setJson(json);
	}

	public int compareTo(MicroschemaVersion version) {
		return delegate.compareTo(version);
	}

	public Iterable<Job> referencedJobsViaTo() {
		return delegate.referencedJobsViaTo();
	}

	public Iterable<Job> referencedJobsViaFrom() {
		return delegate.referencedJobsViaFrom();
	}

	public Set<String> getRoleUuidsForPerm(InternalPermission permission) {
		return delegate.getRoleUuidsForPerm(permission);
	}

	public void setRoleUuidForPerm(InternalPermission permission, Set<String> allowedRoles) {
		delegate.setRoleUuidForPerm(permission, allowedRoles);
	}

}
