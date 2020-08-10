package com.gentics.mesh.core.data.impl;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.HibBranch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchMicroschemaAssignModel;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchTaggedEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectBranchEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.tag.TagReference;
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

public class BranchWrapper implements Branch, HibBranch {

	private final Branch delegate;

	public static BranchWrapper wrap(Branch branch) {
		if (branch == null) {
			return null;
		} else {
			return new BranchWrapper(branch);
		}
	}

	public BranchWrapper(Branch delegate) {
		this.delegate = delegate;
	}

	public Object id() {
		return delegate.id();
	}

	public void setCreated(User creator) {
		delegate.setCreated(creator);
	}

	public BranchReference transformToReference() {
		return delegate.transformToReference();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return delegate.getRolePermissions(ac, roleUuid);
	}

	public User getEditor() {
		return delegate.getEditor();
	}

	public User getCreator() {
		return delegate.getCreator();
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

	public void setEditor(User user) {
		delegate.setEditor(user);
	}

	public void setCreator(User user) {
		delegate.setCreator(user);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return delegate.getRolesWithPerm(perm);
	}

	public String getUuid() {
		return delegate.getUuid();
	}

	public void setName(String name) {
		delegate.setName(name);
	}

	public Single<BranchResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return delegate.transformToRest(ac, level, languageTags);
	}

	public Vertex getVertex() {
		return delegate.getVertex();
	}

	public String getElementVersion() {
		return delegate.getElementVersion();
	}

	public Long getLastEditedTimestamp() {
		return delegate.getLastEditedTimestamp();
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

	public void setLastEditedTimestamp(long timestamp) {
		delegate.setLastEditedTimestamp(timestamp);
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

	public Project getProject() {
		return delegate.getProject();
	}

	public void delete() {
		delegate.delete();
	}

	public BranchResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		return delegate.transformToRestSync(ac, level, languageTags);
	}

	public void setCreationTimestamp(long timestamp) {
		delegate.setCreationTimestamp(timestamp);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return delegate.out(label, clazz);
	}

	public void setLastEditedTimestamp() {
		delegate.setLastEditedTimestamp();
	}

	public FramedGraph getGraph() {
		return delegate.getGraph();
	}

	public List<Tag> getTagsToSet(InternalActionContext ac, EventQueueBatch batch) {
		return delegate.getTagsToSet(ac, batch);
	}

	public <R> void property(String key, R value) {
		delegate.property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
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

	public String getLastEditedDate() {
		return delegate.getLastEditedDate();
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

	public String getETag(InternalActionContext ac) {
		return delegate.getETag(ac);
	}

	public Vertx vertx() {
		return delegate.vertx();
	}

	public boolean hasPublishPermissions() {
		return delegate.hasPublishPermissions();
	}

	public List<Tag> getTagsToSet(List<TagReference> list, InternalActionContext ac, EventQueueBatch batch) {
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

	public TypeInfo getTypeInfo() {
		return delegate.getTypeInfo();
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return delegate.addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return delegate.getGraphAttribute(key);
	}

	public boolean isActive() {
		return delegate.isActive();
	}

	public Branch setActive(boolean active) {
		return delegate.setActive(active);
	}

	public boolean isMigrated() {
		return delegate.isMigrated();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return delegate.in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return delegate.outE(labels);
	}

	public Branch setMigrated(boolean migrated) {
		return delegate.setMigrated(migrated);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return delegate.inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.linkOut(vertex, labels);
	}

	public String getHostname() {
		return delegate.getHostname();
	}

	public Branch setHostname(String hostname) {
		return delegate.setHostname(hostname);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.linkIn(vertex, labels);
	}

	public Boolean getSsl() {
		return delegate.getSsl();
	}

	public Branch setSsl(boolean ssl) {
		return delegate.setSsl(ssl);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.unlinkOut(vertex, labels);
	}

	public String getPathPrefix() {
		return delegate.getPathPrefix();
	}

	public Branch setPathPrefix(String pathPrefix) {
		return delegate.setPathPrefix(pathPrefix);
	}

	public boolean isLatest() {
		return delegate.isLatest();
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.unlinkIn(vertex, labels);
	}

	public Branch setLatest() {
		return delegate.setLatest();
	}

	public Branch getNextBranch() {
		return delegate.getNextBranch();
	}

	public Branch setNextBranch(Branch branch) {
		return delegate.setNextBranch(branch);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.setLinkOut(vertex, labels);
	}

	public Branch getPreviousBranch() {
		return wrap(delegate.getPreviousBranch());
	}

	public BranchRoot getRoot() {
		return delegate.getRoot();
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return delegate.traversal();
	}

	public Job assignSchemaVersion(User user, SchemaVersion schemaVersion, EventQueueBatch batch) {
		return delegate.assignSchemaVersion(user, schemaVersion, batch);
	}

	public JsonObject toJson() {
		return delegate.toJson();
	}

	public <T> T reframe(Class<T> kind) {
		return delegate.reframe(kind);
	}

	public Branch unassignSchema(Schema schemaContainer) {
		return wrap(delegate.unassignSchema(schemaContainer));
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return delegate.reframeExplicit(kind);
	}

	public boolean contains(Schema schema) {
		return delegate.contains(schema);
	}

	public boolean contains(SchemaVersion schemaVersion) {
		return delegate.contains(schemaVersion);
	}

	@Override
	public Tag findTagByUuid(String uuid) {
		return delegate.findTagByUuid(uuid);
	}

	public TraversalResult<? extends SchemaVersion> findAllSchemaVersions() {
		return delegate.findAllSchemaVersions();
	}

	public Job assignMicroschemaVersion(User user, MicroschemaVersion microschemaVersion, EventQueueBatch batch) {
		return delegate.assignMicroschemaVersion(user, microschemaVersion, batch);
	}

	public Branch unassignMicroschema(Microschema microschema) {
		return delegate.unassignMicroschema(microschema);
	}

	public boolean contains(Microschema microschema) {
		return delegate.contains(microschema);
	}

	public boolean contains(MicroschemaVersion microschemaVersion) {
		return delegate.contains(microschemaVersion);
	}

	public TraversalResult<? extends MicroschemaVersion> findAllMicroschemaVersions() {
		return delegate.findAllMicroschemaVersions();
	}

	public TraversalResult<? extends BranchMicroschemaEdge> findAllLatestMicroschemaVersionEdges() {
		return delegate.findAllLatestMicroschemaVersionEdges();
	}

	public TraversalResult<? extends SchemaVersion> findActiveSchemaVersions() {
		return delegate.findActiveSchemaVersions();
	}

	public Iterable<? extends MicroschemaVersion> findActiveMicroschemaVersions() {
		return delegate.findActiveMicroschemaVersions();
	}

	public Iterable<? extends BranchSchemaEdge> findAllLatestSchemaVersionEdges() {
		return delegate.findAllLatestSchemaVersionEdges();
	}

	public Branch setProject(Project project) {
		return delegate.setProject(project);
	}

	public TraversalResult<? extends BranchSchemaEdge> findAllSchemaVersionEdges() {
		return delegate.findAllSchemaVersionEdges();
	}

	public TraversalResult<? extends BranchMicroschemaEdge> findAllMicroschemaVersionEdges() {
		return delegate.findAllMicroschemaVersionEdges();
	}

	public BranchSchemaEdge findBranchSchemaEdge(SchemaVersion schemaVersion) {
		return delegate.findBranchSchemaEdge(schemaVersion);
	}

	public BranchMicroschemaEdge findBranchMicroschemaEdge(MicroschemaVersion microschemaVersion) {
		return delegate.findBranchMicroschemaEdge(microschemaVersion);
	}

	public SchemaVersion findLatestSchemaVersion(Schema schemaContainer) {
		return delegate.findLatestSchemaVersion(schemaContainer);
	}

	public MicroschemaVersion findLatestMicroschemaVersion(Microschema schemaContainer) {
		return delegate.findLatestMicroschemaVersion(schemaContainer);
	}

	public void addTag(Tag tag) {
		delegate.addTag(tag);
	}

	public void removeTag(Tag tag) {
		delegate.removeTag(tag);
	}

	public void removeAllTags() {
		delegate.removeAllTags();
	}

	public TraversalResult<? extends Tag> getTags() {
		return delegate.getTags();
	}

	public TransformablePage<? extends Tag> getTags(User user, PagingParameters params) {
		return delegate.getTags(user, params);
	}

	public boolean hasTag(Tag tag) {
		return delegate.hasTag(tag);
	}

	public TransformablePage<? extends Tag> updateTags(InternalActionContext ac, EventQueueBatch batch) {
		return delegate.updateTags(ac, batch);
	}

	public ProjectBranchEventModel onSetLatest() {
		return delegate.onSetLatest();
	}

	public BranchTaggedEventModel onTagged(Tag tag, Assignment assignment) {
		return delegate.onTagged(tag, assignment);
	}

	public BranchSchemaAssignEventModel onSchemaAssignEvent(SchemaVersion schemaVersion, Assignment assigned, JobStatus status) {
		return delegate.onSchemaAssignEvent(schemaVersion, assigned, status);
	}

	public BranchMicroschemaAssignModel onMicroschemaAssignEvent(MicroschemaVersion microschemaVersion, Assignment assigned,
		JobStatus status) {
		return delegate.onMicroschemaAssignEvent(microschemaVersion, assigned, status);
	}

}
