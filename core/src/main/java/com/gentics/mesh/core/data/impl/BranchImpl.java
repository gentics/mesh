package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.Events.JOB_WORKER_ADDRESS;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NEXT_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.QUEUED;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.graphdb.spi.FieldType.STRING;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.branch.ReleaseVersionEdge;
import com.gentics.mesh.core.data.branch.impl.BranchMicroschemaEdgeImpl;
import com.gentics.mesh.core.data.branch.impl.BranchSchemaEdgeImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.impl.ReleaseRootImpl;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.VersionUtil;

import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * @see Branch
 */
public class BranchImpl extends AbstractMeshCoreVertex<BranchResponse, Branch> implements Branch {

	public static final String UNIQUENAME_PROPERTY_KEY = "uniqueName";

	public static final String UNIQUENAME_INDEX_NAME = "uniqueReleaseNameIndex";

	public static final String ACTIVE_PROPERTY_KEY = "active";

	public static final String MIGRATED_PROPERTY_KEY = "migrated";

	public static void init(Database database) {
		database.addVertexType(BranchImpl.class, MeshVertexImpl.class);
		database.addVertexIndex(UNIQUENAME_INDEX_NAME, BranchImpl.class, true, UNIQUENAME_PROPERTY_KEY, STRING);
	}

	@Override
	public BranchReference transformToReference() {
		return new BranchReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	public boolean update(InternalActionContext ac, SearchQueueBatch batch) {
		Database db = MeshInternal.get().database();
		BranchUpdateRequest requestModel = ac.fromJson(BranchUpdateRequest.class);
		boolean modified = false;

		if (shouldUpdate(requestModel.getName(), getName())) {
			// Check for conflicting project name
			Branch conflictingRelease = db.checkIndexUniqueness(UNIQUENAME_INDEX_NAME, this, getRoot().getUniqueNameKey(requestModel.getName()));
			if (conflictingRelease != null) {
				throw conflict(conflictingRelease.getUuid(), conflictingRelease.getName(), "release_conflicting_name", requestModel.getName());
			}
			setName(requestModel.getName());
			modified = true;
		}

		if (shouldUpdate(requestModel.getHostname(), getHostname())) {
			setHostname(requestModel.getHostname());
			modified = true;
		}

		if (requestModel.getSsl() != null && requestModel.getSsl() != getSsl()) {
			setSsl(requestModel.getSsl());
			modified = true;
		}

		if (modified) {
			setEditor(ac.getUser());
			setLastEditedTimestamp();
		}
		return modified;
	}

	@Override
	public BranchResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {

		BranchResponse restBranch = new BranchResponse();
		restBranch.setName(getName());
		restBranch.setHostname(getHostname());
		restBranch.setSsl(getSsl());
		// restRelease.setActive(isActive());
		restBranch.setMigrated(isMigrated());

		// Add common fields
		fillCommonRestFields(ac, restBranch);

		// Role permissions
		setRolePermissions(ac, restBranch);

		// Merge and complete
		return restBranch;
	}

	@Override
	public String getName() {
		return getProperty(NAME);
	}

	@Override
	public void setName(String name) {
		setProperty(NAME, name);
		setProperty(UNIQUENAME_PROPERTY_KEY, getRoot().getUniqueNameKey(name));
	}

	@Override
	public String getHostname() {
		return getProperty(HOSTNAME);
	}

	@Override
	public Branch setHostname(String hostname) {
		setProperty(HOSTNAME, hostname);
		return this;
	}

	@Override
	public Boolean getSsl() {
		return getProperty(SSL);
	}

	@Override
	public Branch setSsl(boolean ssl) {
		setProperty(SSL, ssl);
		return this;
	}

	@Override
	public boolean isActive() {
		return getProperty(ACTIVE_PROPERTY_KEY);
	}

	@Override
	public Branch setActive(boolean active) {
		setProperty(ACTIVE_PROPERTY_KEY, active);
		return this;
	}

	@Override
	public boolean isMigrated() {
		Boolean flag = getProperty(MIGRATED_PROPERTY_KEY);
		return flag == null ? false : flag;
	}

	@Override
	public Branch setMigrated(boolean migrated) {
		setProperty(MIGRATED_PROPERTY_KEY, migrated);
		return this;
	}

	@Override
	public Branch getNextBranch() {
		return out(HAS_NEXT_RELEASE).nextOrDefaultExplicit(BranchImpl.class, null);
	}

	@Override
	public Branch setNextBranch(Branch branch) {
		setUniqueLinkOutTo(branch, HAS_NEXT_RELEASE);
		return this;
	}

	@Override
	public Branch getPreviousBranch() {
		return in(HAS_NEXT_RELEASE).nextOrDefaultExplicit(BranchImpl.class, null);
	}

	@Override
	public BranchRoot getRoot() {
		return in(HAS_RELEASE).nextOrDefaultExplicit(ReleaseRootImpl.class, null);
	}

	@Override
	public Branch unassignSchema(SchemaContainer schemaContainer) {
		unassign(schemaContainer);
		return this;
	}

	@Override
	public boolean contains(SchemaContainer schemaContainer) {
		SchemaContainer foundSchemaContainer = out(HAS_SCHEMA_VERSION).in(HAS_PARENT_CONTAINER).has("uuid", schemaContainer.getUuid())
				.nextOrDefaultExplicit(SchemaContainerImpl.class, null);
		return foundSchemaContainer != null;
	}

	@Override
	public boolean contains(SchemaContainerVersion schemaContainerVersion) {
		SchemaContainerVersion foundSchemaContainerVersion = out(HAS_SCHEMA_VERSION).retain(schemaContainerVersion).nextOrDefaultExplicit(
				SchemaContainerVersionImpl.class, null);
		return foundSchemaContainerVersion != null;
	}

	@Override
	public SchemaContainerVersion findLatestSchemaVersion(SchemaContainer schemaContainer) {
		return out(HAS_SCHEMA_VERSION).mark().in(HAS_PARENT_CONTAINER).retain(schemaContainer).back().order((o1, o2) -> {
			String v1 = o1.getProperty(GraphFieldSchemaContainerVersion.VERSION_PROPERTY_KEY);
			String v2 = o2.getProperty(GraphFieldSchemaContainerVersion.VERSION_PROPERTY_KEY);
			return VersionUtil.compareVersions(v2, v1);
		}).nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	@Override
	public MicroschemaContainerVersion findLatestMicroschemaVersion(MicroschemaContainer schemaContainer) {
		return out(HAS_MICROSCHEMA_VERSION).mark().in(HAS_PARENT_CONTAINER).retain(schemaContainer).back().order((o1, o2) -> {
			String v1 = o1.getProperty(GraphFieldSchemaContainerVersion.VERSION_PROPERTY_KEY);
			String v2 = o2.getProperty(GraphFieldSchemaContainerVersion.VERSION_PROPERTY_KEY);
			return VersionUtil.compareVersions(v2, v1);
		}).nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

	@Override
	public Iterable<? extends SchemaContainerVersion> findAllSchemaVersions() {
		return out(HAS_SCHEMA_VERSION).frameExplicit(SchemaContainerVersionImpl.class);
	}

	@Override
	public Iterable<? extends SchemaContainerVersion> findActiveSchemaVersions() {
		return outE(HAS_SCHEMA_VERSION).has(ReleaseVersionEdge.ACTIVE_PROPERTY_KEY, true).inV().frameExplicit(SchemaContainerVersionImpl.class);
	}

	@Override
	public Iterable<? extends BranchSchemaEdge> findAllSchemaVersionEdges() {
		return outE(HAS_SCHEMA_VERSION).frameExplicit(BranchSchemaEdgeImpl.class);
	}

	@Override
	public Iterable<? extends BranchMicroschemaEdge> findAllMicroschemaVersionEdges() {
		return outE(HAS_MICROSCHEMA_VERSION).frameExplicit(BranchMicroschemaEdgeImpl.class);
	}

	@Override
	public Iterable<? extends BranchMicroschemaEdge> findAllLatestMicroschemaVersionEdges() {
		// Locate one version (latest) of all versions per schema
		return Observable.fromIterable(outE(HAS_MICROSCHEMA_VERSION).frameExplicit(BranchMicroschemaEdgeImpl.class)).groupBy(it -> it
				.getMicroschemaContainerVersion().getSchemaContainer().getUuid()).flatMapMaybe(it -> it.reduce((a, b) -> a
						.getMicroschemaContainerVersion().compareTo(b.getMicroschemaContainerVersion()) > 0 ? a : b)).blockingIterable();
	}

	@Override
	public Iterable<? extends BranchSchemaEdge> findAllLatestSchemaVersionEdges() {
		// Locate one version (latest) of all versions per schema
		return Observable.fromIterable(outE(HAS_SCHEMA_VERSION).frameExplicit(BranchSchemaEdgeImpl.class)).groupBy(it -> it
				.getSchemaContainerVersion().getSchemaContainer().getUuid()).flatMapMaybe(it -> it.reduce((a, b) -> a.getSchemaContainerVersion()
						.compareTo(b.getSchemaContainerVersion()) > 0 ? a : b)).blockingIterable();
	}

	@Override
	public Job assignSchemaVersion(User user, SchemaContainerVersion schemaContainerVersion) {
		BranchSchemaEdge edge = findReleaseSchemaEdge(schemaContainerVersion);
		// Don't remove any existing edge. Otherwise the edge properties are lost
		if (edge == null) {
			SchemaContainerVersion currentVersion = findLatestSchemaVersion(schemaContainerVersion.getSchemaContainer());
			edge = addFramedEdgeExplicit(HAS_SCHEMA_VERSION, schemaContainerVersion, BranchSchemaEdgeImpl.class);
			// Enqueue the schema migration for each found schema version
			edge.setActive(true);
			if (currentVersion != null) {
				Job job = MeshInternal.get().boot().jobRoot().enqueueSchemaMigration(user, this, currentVersion, schemaContainerVersion);
				edge.setMigrationStatus(QUEUED);
				edge.setJobUuid(job.getUuid());
				return job;
			} else {
				// No migration needed since there was no previous version assigned.
				edge.setMigrationStatus(COMPLETED);
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public Job assignMicroschemaVersion(User user, MicroschemaContainerVersion microschemaContainerVersion) {
		BranchMicroschemaEdge edge = findReleaseMicroschemaEdge(microschemaContainerVersion);
		// Don't remove any existing edge. Otherwise the edge properties are lost
		if (edge == null) {
			MicroschemaContainerVersion currentVersion = findLatestMicroschemaVersion(microschemaContainerVersion.getSchemaContainer());
			edge = addFramedEdgeExplicit(HAS_MICROSCHEMA_VERSION, microschemaContainerVersion, BranchMicroschemaEdgeImpl.class);
			// Enqueue the job so that the worker can process it later on
			edge.setActive(true);
			if (currentVersion != null) {
				Job job = MeshInternal.get().boot().jobRoot().enqueueMicroschemaMigration(user, this, currentVersion, microschemaContainerVersion);
				edge.setMigrationStatus(QUEUED);
				edge.setJobUuid(job.getUuid());
				return job;
			} else {
				// No migration needed since there was no previous version assigned.
				edge.setMigrationStatus(COMPLETED);
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public Branch unassignMicroschema(MicroschemaContainer microschemaContainer) {
		unassign(microschemaContainer);
		return this;
	}

	@Override
	public boolean contains(MicroschemaContainer microschema) {
		MicroschemaContainer foundMicroschemaContainer = out(HAS_MICROSCHEMA_VERSION).in(HAS_PARENT_CONTAINER).has("uuid", microschema.getUuid())
				.nextOrDefaultExplicit(MicroschemaContainerImpl.class, null);
		return foundMicroschemaContainer != null;
	}

	@Override
	public boolean contains(MicroschemaContainerVersion microschemaContainerVersion) {
		MicroschemaContainerVersion foundMicroschemaContainerVersion = out(HAS_MICROSCHEMA_VERSION).has("uuid", microschemaContainerVersion.getUuid())
				.nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
		return foundMicroschemaContainerVersion != null;
	}

	@Override
	public Iterable<? extends MicroschemaContainerVersion> findAllMicroschemaVersions() {
		return out(HAS_MICROSCHEMA_VERSION).frameExplicit(MicroschemaContainerVersionImpl.class);
	}

	/**
	 * Unassigns the latest version of the container from the branch.
	 * 
	 * @param container
	 *            Container to handle
	 */
	protected <R extends FieldSchemaContainer, RM extends FieldSchemaContainer, RE extends NameUuidReference<RE>, SCV extends GraphFieldSchemaContainerVersion<R, RM, RE, SCV, SC>, SC extends GraphFieldSchemaContainer<R, RE, SC, SCV>> void unassign(
			GraphFieldSchemaContainer<R, RE, SC, SCV> container) {
		SCV version = container.getLatestVersion();
		String edgeLabel = null;
		if (version instanceof SchemaContainerVersion) {
			edgeLabel = HAS_SCHEMA_VERSION;
		}
		if (version instanceof MicroschemaContainerVersion) {
			edgeLabel = HAS_MICROSCHEMA_VERSION;
		}

		// Iterate over all versions of the container and unassign it from the
		// branch. We don't know which version was assigned to the branch
		// so we just unassign all versions of the container.
		while (version != null) {
			unlinkOut(version, edgeLabel);
			version = version.getPreviousVersion();
		}
	}

	@Override
	public String getETag(InternalActionContext ac) {
		return ETag.hash(getUuid() + "-" + getLastEditedTimestamp());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/" + encodeSegment(getProject().getName()) + "/branches/" + getUuid();
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public Branch setProject(Project project) {
		setUniqueLinkOutTo(project, ASSIGNED_TO_PROJECT);
		return this;
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR).nextOrDefaultExplicit(UserImpl.class, null);
	}

	@Override
	public Single<BranchResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return DB.get().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

	@Override
	public BranchSchemaEdge findReleaseSchemaEdge(SchemaContainerVersion schemaContainerVersion) {
		return outE(HAS_SCHEMA_VERSION).mark().inV().retain(schemaContainerVersion).back().nextOrDefaultExplicit(BranchSchemaEdgeImpl.class, null);
	}

	@Override
	public BranchMicroschemaEdge findReleaseMicroschemaEdge(MicroschemaContainerVersion microschemaContainerVersion) {
		return outE(HAS_MICROSCHEMA_VERSION).mark().inV().retain(microschemaContainerVersion).back().nextOrDefaultExplicit(
				BranchMicroschemaEdgeImpl.class, null);
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		// TODO Do we need to delete affected nodes as well? Currently only deletion of projects is possible. Release can't be deleted without deleting the
		// project.
		getVertex().remove();
	}

	@Override
	public void onCreated() {
		super.onCreated();
		// TODO make this configurable via query parameter. It should be possible to postpone the migration.
		Mesh.vertx().eventBus().send(JOB_WORKER_ADDRESS, null);
	}
}
