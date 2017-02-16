package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NEXT_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.util.URIUtils.encodeFragment;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
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
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.ETag;

import rx.Single;

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
		database.addVertexIndex(UNIQUENAME_INDEX_NAME, ReleaseImpl.class, true, UNIQUENAME_PROPERTY_KEY);
	}

	@Override
	public String getType() {
		return Release.TYPE;
	}

	@Override
	public ReleaseReference transformToReference() {
		return new ReleaseReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	public Release update(InternalActionContext ac, SearchQueueBatch batch) {
		Database db = MeshInternal.get().database();
		ReleaseUpdateRequest requestModel = ac.fromJson(ReleaseUpdateRequest.class);

		if (shouldUpdate(requestModel.getName(), getName())) {
			// Check for conflicting project name
			Release conflictingRelease = db.checkIndexUniqueness(UNIQUENAME_INDEX_NAME, this, getRoot().getUniqueNameKey(requestModel.getName()));
			if (conflictingRelease != null) {
				throw conflict(conflictingRelease.getUuid(), conflictingRelease.getName(), "release_conflicting_name", requestModel.getName());
			}
			setName(requestModel.getName());
			setEditor(ac.getUser());
			setLastEditedTimestamp();
		}
		// TODO: Not yet fully implemented
		// if (requestModel.getActive() != null) {
		// setActive(requestModel.getActive());
		// }
		return this;
	}

	@Override
	public ReleaseResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {

		ReleaseResponse restRelease = new ReleaseResponse();
		restRelease.setName(getName());
		// restRelease.setActive(isActive());
		restRelease.setMigrated(isMigrated());

		// Add common fields
		fillCommonRestFields(ac, restRelease);

		// Role permissions
		setRolePermissions(ac, restRelease);

		// Merge and complete
		return restRelease;
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
		setProperty(UNIQUENAME_PROPERTY_KEY, getRoot().getUniqueNameKey(name));
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
		return getProperty(MIGRATED_PROPERTY_KEY);
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
	public Release assignSchemaVersion(SchemaContainerVersion schemaContainerVersion) {
		assign(schemaContainerVersion);
		return this;
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
		SchemaContainerVersion foundSchemaContainerVersion = out(HAS_SCHEMA_VERSION).has("uuid", schemaContainerVersion.getUuid())
				.nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
		return foundSchemaContainerVersion != null;
	}

	@Override
	public SchemaContainerVersion getVersion(SchemaContainer schemaContainer) {
		return out(HAS_SCHEMA_VERSION).mark().in(HAS_PARENT_CONTAINER).has("uuid", schemaContainer.getUuid()).back()
				.nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	@Override
	public List<? extends SchemaContainerVersion> findAllSchemaVersions() {
		return out(HAS_SCHEMA_VERSION).toListExplicit(SchemaContainerVersionImpl.class);
	}

	@Override
	public Release assignMicroschemaVersion(MicroschemaContainerVersion microschemaContainerVersion) {
		assign(microschemaContainerVersion);
		return this;
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
	public MicroschemaContainerVersion getVersion(MicroschemaContainer microschemaContainer) {
		return out(HAS_MICROSCHEMA_VERSION).mark().in(HAS_PARENT_CONTAINER).has("uuid", microschemaContainer.getUuid()).back()
				.nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

	@Override
	public List<? extends MicroschemaContainerVersion> findAllMicroschemaVersions() throws InvalidArgumentException {
		return out(HAS_MICROSCHEMA_VERSION).toListExplicit(MicroschemaContainerVersionImpl.class);
	}

	/**
	 * Assign the given schema container version to this release and unassign all other versions.
	 * 
	 * @param version
	 *            version to assign
	 */
	protected <R extends FieldSchemaContainer, RM extends FieldSchemaContainer, RE extends NameUuidReference<RE>, SCV extends GraphFieldSchemaContainerVersion<R, RM, RE, SCV, SC>, SC extends GraphFieldSchemaContainer<R, RE, SC, SCV>> void assign(
			GraphFieldSchemaContainerVersion<R, RM, RE, SCV, SC> version) {
		String edgeLabel = null;
		if (version instanceof SchemaContainerVersion) {
			edgeLabel = HAS_SCHEMA_VERSION;
		}
		if (version instanceof MicroschemaContainerVersion) {
			edgeLabel = HAS_MICROSCHEMA_VERSION;
		}

		setUniqueLinkOutTo(version, edgeLabel);

		// unlink all other versions
		SCV previous = version.getPreviousVersion();
		while (previous != null) {
			unlinkOut(previous, edgeLabel);
			previous = previous.getPreviousVersion();
		}

		SCV next = version.getNextVersion();
		while (next != null) {
			unlinkOut(next, edgeLabel);
			next = next.getNextVersion();
		}
	}

	/**
	 * Unassigns all version of the container from the release.
	 * 
	 * @param container
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
		// release
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
		return "/api/v1/" + encodeFragment(getProject().getName()) + "/releases/" + getUuid();
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
		return db.operateNoTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}
}
