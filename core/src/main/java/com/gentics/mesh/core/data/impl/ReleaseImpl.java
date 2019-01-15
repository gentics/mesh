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
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.release.ReleaseMicroschemaEdge;
import com.gentics.mesh.core.data.release.ReleaseSchemaEdge;
import com.gentics.mesh.core.data.release.ReleaseVersionEdge;
import com.gentics.mesh.core.data.release.impl.ReleaseMicroschemaEdgeImpl;
import com.gentics.mesh.core.data.release.impl.ReleaseSchemaEdgeImpl;
import com.gentics.mesh.core.data.root.ReleaseRoot;
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
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.release.ReleaseReference;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.ReleaseUpdateRequest;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.VersionUtil;

import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * @see Release
 */
public class ReleaseImpl extends AbstractMeshCoreVertex<ReleaseResponse, Release> implements Release {

	public static final String UNIQUENAME_PROPERTY_KEY = "uniqueName";

	public static final String UNIQUENAME_INDEX_NAME = "uniqueReleaseNameIndex";

	public static final String ACTIVE_PROPERTY_KEY = "active";

	public static final String MIGRATED_PROPERTY_KEY = "migrated";

	public static void init(Database database) {
		database.addVertexType(ReleaseImpl.class, MeshVertexImpl.class);
		database.addVertexIndex(UNIQUENAME_INDEX_NAME, ReleaseImpl.class, true, UNIQUENAME_PROPERTY_KEY, STRING);
	}

	@Override
	public ReleaseReference transformToReference() {
		return new ReleaseReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	public boolean update(InternalActionContext ac, SearchQueueBatch batch) {
		Database db = MeshInternal.get().database();
		ReleaseUpdateRequest requestModel = ac.fromJson(ReleaseUpdateRequest.class);
		boolean modified = false;

		if (shouldUpdate(requestModel.getName(), getName())) {
			// Check for conflicting project name
			Release conflictingRelease = db.checkIndexUniqueness(UNIQUENAME_INDEX_NAME, this, getRoot().getUniqueNameKey(requestModel.getName()));
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
	public ReleaseResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		ReleaseResponse restRelease = new ReleaseResponse();
		if (fields.has("name")) {
			restRelease.setName(getName());
		}
		if (fields.has("hostname")) {
			restRelease.setHostname(getHostname());
		}
		if (fields.has("ssl")) {
			restRelease.setSsl(getSsl());
		}
		// restRelease.setActive(isActive());
		if (fields.has("migrated")) {
			restRelease.setMigrated(isMigrated());
		}

		// Add common fields
		fillCommonRestFields(ac, fields, restRelease);

		// Role permissions
		setRolePermissions(ac, restRelease);
		return restRelease;
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
	public Release setHostname(String hostname) {
		setProperty(HOSTNAME, hostname);
		return this;
	}

	@Override
	public Boolean getSsl() {
		return getProperty(SSL);
	}

	@Override
	public Release setSsl(boolean ssl) {
		setProperty(SSL, ssl);
		return this;
	}

	@Override
	public boolean isActive() {
		return getProperty(ACTIVE_PROPERTY_KEY);
	}

	@Override
	public Release setActive(boolean active) {
		setProperty(ACTIVE_PROPERTY_KEY, active);
		return this;
	}

	@Override
	public boolean isMigrated() {
		Boolean flag = getProperty(MIGRATED_PROPERTY_KEY);
		return flag == null ? false : flag;
	}

	@Override
	public Release setMigrated(boolean migrated) {
		setProperty(MIGRATED_PROPERTY_KEY, migrated);
		return this;
	}

	@Override
	public Release getNextRelease() {
		return out(HAS_NEXT_RELEASE).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public Release setNextRelease(Release release) {
		setUniqueLinkOutTo(release, HAS_NEXT_RELEASE);
		return this;
	}

	@Override
	public Release getPreviousRelease() {
		return in(HAS_NEXT_RELEASE).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public ReleaseRoot getRoot() {
		return in(HAS_RELEASE).nextOrDefaultExplicit(ReleaseRootImpl.class, null);
	}

	@Override
	public Release unassignSchema(SchemaContainer schemaContainer) {
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
	public Iterable<? extends ReleaseSchemaEdge> findAllSchemaVersionEdges() {
		return outE(HAS_SCHEMA_VERSION).frameExplicit(ReleaseSchemaEdgeImpl.class);
	}

	@Override
	public Iterable<? extends ReleaseMicroschemaEdge> findAllMicroschemaVersionEdges() {
		return outE(HAS_MICROSCHEMA_VERSION).frameExplicit(ReleaseMicroschemaEdgeImpl.class);
	}

	@Override
	public Iterable<? extends ReleaseMicroschemaEdge> findAllLatestMicroschemaVersionEdges() {
		// Locate one version (latest) of all versions per schema
		return Observable.fromIterable(outE(HAS_MICROSCHEMA_VERSION).frameExplicit(ReleaseMicroschemaEdgeImpl.class)).groupBy(it -> it
			.getMicroschemaContainerVersion().getSchemaContainer().getUuid()).flatMapMaybe(it -> it.reduce(
				(a, b) -> a
					.getMicroschemaContainerVersion().compareTo(b.getMicroschemaContainerVersion()) > 0 ? a : b))
			.blockingIterable();
	}

	@Override
	public Iterable<? extends ReleaseSchemaEdge> findAllLatestSchemaVersionEdges() {
		// Locate one version (latest) of all versions per schema
		return Observable.fromIterable(outE(HAS_SCHEMA_VERSION).frameExplicit(ReleaseSchemaEdgeImpl.class)).groupBy(it -> it
			.getSchemaContainerVersion().getSchemaContainer().getUuid()).flatMapMaybe(it -> it.reduce(
				(a, b) -> a.getSchemaContainerVersion()
					.compareTo(b.getSchemaContainerVersion()) > 0 ? a : b))
			.blockingIterable();
	}

	@Override
	public Job assignSchemaVersion(User user, SchemaContainerVersion schemaContainerVersion) {
		ReleaseSchemaEdge edge = findReleaseSchemaEdge(schemaContainerVersion);
		// Don't remove any existing edge. Otherwise the edge properties are lost
		if (edge == null) {
			SchemaContainerVersion currentVersion = findLatestSchemaVersion(schemaContainerVersion.getSchemaContainer());
			edge = addFramedEdgeExplicit(HAS_SCHEMA_VERSION, schemaContainerVersion, ReleaseSchemaEdgeImpl.class);
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
		ReleaseMicroschemaEdge edge = findReleaseMicroschemaEdge(microschemaContainerVersion);
		// Don't remove any existing edge. Otherwise the edge properties are lost
		if (edge == null) {
			MicroschemaContainerVersion currentVersion = findLatestMicroschemaVersion(microschemaContainerVersion.getSchemaContainer());
			edge = addFramedEdgeExplicit(HAS_MICROSCHEMA_VERSION, microschemaContainerVersion, ReleaseMicroschemaEdgeImpl.class);
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
	public Release unassignMicroschema(MicroschemaContainer microschemaContainer) {
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
	 * Unassigns the latest version of the container from the release.
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
		// release. We don't know which version was assigned to the release
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
		return "/api/v1/" + encodeSegment(getProject().getName()) + "/releases/" + getUuid();
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public Release setProject(Project project) {
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
	public Single<ReleaseResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return DB.get().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

	@Override
	public ReleaseSchemaEdge findReleaseSchemaEdge(SchemaContainerVersion schemaContainerVersion) {
		return outE(HAS_SCHEMA_VERSION).mark().inV().retain(schemaContainerVersion).back().nextOrDefaultExplicit(ReleaseSchemaEdgeImpl.class, null);
	}

	@Override
	public ReleaseMicroschemaEdge findReleaseMicroschemaEdge(MicroschemaContainerVersion microschemaContainerVersion) {
		return outE(HAS_MICROSCHEMA_VERSION).mark().inV().retain(microschemaContainerVersion).back().nextOrDefaultExplicit(
			ReleaseMicroschemaEdgeImpl.class, null);
	}

	@Override
	public void delete(BulkActionContext bac) {
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
