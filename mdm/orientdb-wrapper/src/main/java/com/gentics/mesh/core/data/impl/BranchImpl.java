package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NEXT_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.madl.field.FieldType.STRING;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.branch.BranchVersionEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.impl.BranchMicroschemaEdgeImpl;
import com.gentics.mesh.core.data.branch.impl.BranchSchemaEdgeImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.impl.BranchRootImpl;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.event.branch.BranchMeshEventModel;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.VersionUtil;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.reactivex.Observable;

/**
 * @see Branch
 */
public class BranchImpl extends AbstractMeshCoreVertex<BranchResponse> implements Branch {

	public static final String UNIQUENAME_PROPERTY_KEY = "uniqueName";

	public static final String ACTIVE_PROPERTY_KEY = "active";

	public static final String MIGRATED_PROPERTY_KEY = "migrated";

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(BranchImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(BranchImpl.class)
			.withName(Branch.UNIQUENAME_INDEX_NAME)
			.withField(UNIQUENAME_PROPERTY_KEY, STRING)
			.unique());
		// database.addVertexIndex(UNIQUENAME_INDEX_NAME, BranchImpl.class, true, UNIQUENAME_PROPERTY_KEY, STRING);
	}

	@Override
	public BranchReference transformToReference() {
		return new BranchReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	public String getName() {
		return property(NAME);
	}

	@Override
	public void setName(String name) {
		property(NAME, name);
		property(UNIQUENAME_PROPERTY_KEY, getRoot().getUniqueNameKey(name));
	}

	@Override
	public String getHostname() {
		return property(HOSTNAME);
	}

	@Override
	public String getPathPrefix() {
		String prefix = property(PATH_PREFIX);
		return prefix == null ? "" : prefix;
	}

	@Override
	public Branch setPathPrefix(String pathPrefix) {
		property(PATH_PREFIX, pathPrefix);
		return this;
	}

	@Override
	public Branch setHostname(String hostname) {
		property(HOSTNAME, hostname);
		return this;
	}

	@Override
	public Boolean getSsl() {
		return property(SSL);
	}

	@Override
	public Branch setSsl(boolean ssl) {
		property(SSL, ssl);
		return this;
	}

	@Override
	public boolean isLatest() {
		return inE(HAS_LATEST_BRANCH).hasNext();
	}

	@Override
	public Branch setLatest() {
		getRoot().setLatestBranch(this);
		return this;
	}

	@Override
	public boolean isActive() {
		return property(ACTIVE_PROPERTY_KEY);
	}

	@Override
	public Branch setActive(boolean active) {
		property(ACTIVE_PROPERTY_KEY, active);
		return this;
	}

	@Override
	public boolean isMigrated() {
		Boolean flag = property(MIGRATED_PROPERTY_KEY);
		return flag == null ? false : flag;
	}

	@Override
	public Branch setMigrated(boolean migrated) {
		property(MIGRATED_PROPERTY_KEY, migrated);
		return this;
	}

	@Override
	public Branch getNextBranch() {
		return out(HAS_NEXT_BRANCH, BranchImpl.class).nextOrNull();
	}

	@Override
	public Branch setNextBranch(HibBranch branch) {
		setUniqueLinkOutTo(toGraph(branch), HAS_NEXT_BRANCH);
		return this;
	}

	@Override
	public Branch getPreviousBranch() {
		return in(HAS_NEXT_BRANCH, BranchImpl.class).nextOrNull();
	}

	@Override
	public BranchRoot getRoot() {
		return in(HAS_BRANCH, BranchRootImpl.class).nextOrNull();
	}

	@Override
	public Branch unassignSchema(HibSchema schemaContainer) {
		unassign(schemaContainer, HAS_SCHEMA_VERSION);
		return this;
	}

	@Override
	public boolean contains(HibSchema schemaContainer) {
		return out(HAS_SCHEMA_VERSION, SchemaContainerVersionImpl.class)
			.stream()
			.filter(version -> {
				return schemaContainer.getUuid().equals(version.getSchemaContainer().getUuid());
			}).findAny().isPresent();
	}

	@Override
	public boolean contains(HibSchemaVersion schemaVersion) {
		return out(HAS_SCHEMA_VERSION, SchemaContainerVersionImpl.class)
			.stream()
			.filter(version -> {
				return schemaVersion.getUuid().equals(version.getUuid());
			}).findAny().isPresent();
	}

	@Override
	public HibSchemaVersion findLatestSchemaVersion(HibSchema schemaContainer) {
		SchemaContainerVersionImpl graphVersion = out(HAS_SCHEMA_VERSION, SchemaContainerVersionImpl.class)
			.stream()
			.filter(version -> {
				return schemaContainer.getUuid().equals(version.getSchemaContainer().getUuid());
			}).sorted((o1, o2) -> {
				String v1 = o1.getProperty(GraphFieldSchemaContainerVersion.VERSION_PROPERTY_KEY);
				String v2 = o2.getProperty(GraphFieldSchemaContainerVersion.VERSION_PROPERTY_KEY);
				return VersionUtil.compareVersions(v2, v1);
			}).findFirst().orElse(null);
		return graphVersion;
	}

	@Override
	public HibMicroschemaVersion findLatestMicroschemaVersion(HibMicroschema schemaContainer) {
		return out(HAS_MICROSCHEMA_VERSION, MicroschemaContainerVersionImpl.class)
			.stream()
			.filter(version -> {
				return schemaContainer.getUuid().equals(version.getSchemaContainer().getUuid());
			}).sorted((o1, o2) -> {
				String v1 = o1.getProperty(GraphFieldSchemaContainerVersion.VERSION_PROPERTY_KEY);
				String v2 = o2.getProperty(GraphFieldSchemaContainerVersion.VERSION_PROPERTY_KEY);
				return VersionUtil.compareVersions(v2, v1);
			}).findFirst().orElse(null);
	}

	@Override
	public Result<? extends HibSchemaVersion> findAllSchemaVersions() {
		return out(HAS_SCHEMA_VERSION, SchemaContainerVersionImpl.class);
	}

	@Override
	public Result<HibSchemaVersion> findActiveSchemaVersions() {
		return new TraversalResult<>(
			outE(HAS_SCHEMA_VERSION).has(BranchVersionEdge.ACTIVE_PROPERTY_KEY, true).inV().frameExplicit(SchemaContainerVersionImpl.class));
	}

	@Override
	public Result<HibMicroschemaVersion> findActiveMicroschemaVersions() {
		return new TraversalResult<>(outE(HAS_MICROSCHEMA_VERSION).has(BranchVersionEdge.ACTIVE_PROPERTY_KEY, true).inV()
			.frameExplicit(MicroschemaContainerVersionImpl.class));
	}

	@Override
	public Result<? extends BranchSchemaEdge> findAllSchemaVersionEdges() {
		return outE(HAS_SCHEMA_VERSION, BranchSchemaEdgeImpl.class);
	}

	@Override
	public Result<? extends BranchMicroschemaEdge> findAllMicroschemaVersionEdges() {
		return outE(HAS_MICROSCHEMA_VERSION, BranchMicroschemaEdgeImpl.class);
	}

	@Override
	public Result<? extends BranchMicroschemaEdge> findAllLatestMicroschemaVersionEdges() {
		// Locate one version (latest) of all versions per schema
		Iterable<BranchMicroschemaEdgeImpl> it2 = Observable
			.fromIterable(outE(HAS_MICROSCHEMA_VERSION).frameExplicit(BranchMicroschemaEdgeImpl.class)).groupBy(it -> it
				.getMicroschemaContainerVersion().getSchemaContainer().getUuid())
			.flatMapMaybe(it -> it.reduce(
				(a, b) -> a
					.getMicroschemaContainerVersion().compareTo(b.getMicroschemaContainerVersion()) > 0 ? a : b))
			.blockingIterable();
		return new TraversalResult<>(it2);
	}

	@Override
	public Iterable<? extends BranchSchemaEdge> findAllLatestSchemaVersionEdges() {
		// Locate one version (latest) of all versions per schema
		return Observable.fromIterable(outE(HAS_SCHEMA_VERSION).frameExplicit(BranchSchemaEdgeImpl.class)).groupBy(it -> it
			.getSchemaContainerVersion().getSchemaContainer().getUuid()).flatMapMaybe(it -> it.reduce(
				(a, b) -> a.getSchemaContainerVersion()
					.compareTo(b.getSchemaContainerVersion()) > 0 ? a : b))
			.blockingIterable();
	}

	@Override
	public HibBranch unassignMicroschema(HibMicroschema microschema) {
		Microschema graphMicroschema = toGraph(microschema);
		unassign(graphMicroschema, HAS_MICROSCHEMA_VERSION);
		return this;
	}

	@Override
	public boolean contains(HibMicroschema microschema) {
		return out(HAS_MICROSCHEMA_VERSION, MicroschemaContainerVersionImpl.class)
			.stream()
			.filter(version -> {
				return microschema.getUuid().equals(version.getSchemaContainer().getUuid());
			}).findAny().isPresent();
	}

	@Override
	public boolean contains(HibMicroschemaVersion microschemaVersion) {
		return out(HAS_MICROSCHEMA_VERSION, MicroschemaContainerVersionImpl.class)
			.stream()
			.filter(version -> {
				return microschemaVersion.getUuid().equals(version.getUuid());
			}).findAny().isPresent();
	}

	@Override
	public Result<? extends HibMicroschemaVersion> findAllMicroschemaVersions() {
		return out(HAS_MICROSCHEMA_VERSION, MicroschemaContainerVersionImpl.class);
	}

	/**
	 * Unassigns the latest version of the container from the branch.
	 * 
	 * @param container
	 *            Container to handle
	 */
	protected <
				R extends FieldSchemaContainer, 
				RM extends FieldSchemaContainerVersion, 
				RE extends NameUuidReference<RE>, 
				SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>, 
				SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>
	> void unassign(HibFieldSchemaElement<R, RM, RE, SC, SCV> container, String edgeLabel) {
		SCV version = container.getLatestVersion();

		// Iterate over all versions of the container and unassign it from the
		// branch. We don't know which version was assigned to the branch
		// so we just unassign all versions of the container.
		while (version != null) {
			unlinkOut(toGraph(version), edgeLabel);
			version = version.getPreviousVersion();
		}
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT, ProjectImpl.class).nextOrNull();
	}

	@Override
	public HibBranch setProject(HibProject project) {
		setUniqueLinkOutTo(toGraph(project), ASSIGNED_TO_PROJECT);
		return this;
	}

	@Override
	public HibUser getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public HibUser getEditor() {
		return mesh().userProperties().getEditor(this);
	}

	@Override
	public BranchSchemaEdge findBranchSchemaEdge(HibSchemaVersion schemaVersion) {
		SchemaVersion graphSchemaVersion = toGraph(schemaVersion);
		return outE(HAS_SCHEMA_VERSION).mark().inV().retain(graphSchemaVersion).back().nextOrDefaultExplicit(BranchSchemaEdgeImpl.class, null);
	}

	@Override
	public BranchMicroschemaEdge findBranchMicroschemaEdge(HibMicroschemaVersion microschemaVersion) {
		MicroschemaVersion graphMicroschemaVersion = toGraph(microschemaVersion);
		return outE(HAS_MICROSCHEMA_VERSION).mark().inV().retain(graphMicroschemaVersion).back().nextOrDefaultExplicit(
			BranchMicroschemaEdgeImpl.class, null);
	}

	@Override
	public void delete(BulkActionContext bac) {
		bac.add(onDeleted());
		getVertex().remove();
	}

	@Override
	public BranchMeshEventModel createEvent(MeshEvent event) {
		return Branch.super.createEvent(event);
	}

	@Override
	public void addTag(HibTag tag) {
		removeTag(tag);
		addFramedEdge(HAS_BRANCH_TAG, toGraph(tag));
	}

	@Override
	public void removeTag(HibTag tag) {
		outE(HAS_BRANCH_TAG).mark().inV().retain(toGraph(tag)).back().removeAll();
	}

	@Override
	public void removeAllTags() {
		outE(HAS_BRANCH_TAG).removeAll();
	}

	@Override
	public Result<? extends Tag> getTags() {
		return new TraversalResult<>(outE(HAS_BRANCH_TAG).inV().frameExplicit(TagImpl.class));
	}

	@Override
	public Tag findTagByUuid(String uuid) {
		return outE(HAS_BRANCH_TAG).inV().has(UUID_KEY, uuid).nextOrDefaultExplicit(TagImpl.class, null);
	}

	@Override
	public Page<? extends Tag> getTags(HibUser user, PagingParameters params) {
		VertexTraversal<?, ?, ?> traversal = outE(HAS_BRANCH_TAG).inV();
		return new DynamicTransformablePageImpl<Tag>(user, traversal, params, READ_PERM, TagImpl.class);
	}

	@Override
	public boolean hasTag(HibTag tag) {
		return outE(HAS_BRANCH_TAG).inV().has(UUID_KEY, tag.getUuid()).hasNext();
	}

	@Override
	@Deprecated
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		return GraphDBTx.getGraphTx().branchDao().update(this.getProject(), this, ac, batch);
	}

	@Override
	public HibBranch setInitial() {
		getRoot().setInitialBranch(this);
		return this;
	}
}
