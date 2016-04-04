package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NEXT_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VERSION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.core.data.root.impl.ReleaseRootImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.release.ReleaseReference;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.ReleaseUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.InvalidArgumentException;

import rx.Observable;

public class ReleaseImpl extends AbstractMeshCoreVertex<ReleaseResponse, Release> implements Release {
	public static final String UNIQUENAME_PROPERTY_KEY = "uniqueName";

	public static final String UNIQUENAME_INDEX_NAME = "uniqueReleaseNameIndex";

	public static void init(Database database) {
		database.addVertexType(ReleaseImpl.class);
		database.addVertexIndex(UNIQUENAME_INDEX_NAME, ReleaseImpl.class, true, UNIQUENAME_PROPERTY_KEY);
	}

	@Override
	public ReleaseReference createEmptyReferenceModel() {
		return new ReleaseReference();
	}

	@Override
	public Observable<? extends Release> update(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		ReleaseUpdateRequest requestModel = ac.fromJson(ReleaseUpdateRequest.class);

		return db.trx(() -> {
			if (shouldUpdate(requestModel.getName(), getName())) {
				// Check for conflicting project name
				Release conflictingRelease = db.checkIndexUniqueness(UNIQUENAME_INDEX_NAME, this,
						getRoot().getUniqueNameKey(requestModel.getName()));
				if (conflictingRelease != null) {
					throw conflict(conflictingRelease.getUuid(), conflictingRelease.getName(),
							"release_conflicting_name", requestModel.getName());
				}
				setName(requestModel.getName());
			}
			if (requestModel.getActive() != null) {
				setActive(requestModel.getActive());
			}
			setEditor(ac.getUser());
			setLastEditedTimestamp(System.currentTimeMillis());
			return Observable.just(this);
		});
	}

	@Override
	public String getType() {
		return Release.TYPE;
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		// TODO Auto-generated method stub

	}

	@Override
	public Observable<ReleaseResponse> transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		Set<Observable<ReleaseResponse>> obsParts = new HashSet<>();

		ReleaseResponse restRelease = new ReleaseResponse();
		restRelease.setName(getName());
		restRelease.setActive(isActive());

		// Add common fields
		obsParts.add(fillCommonRestFields(ac, restRelease));

		// Role permissions
		obsParts.add(setRolePermissions(ac, restRelease));

		// Merge and complete
		return Observable.merge(obsParts).last();
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
		return getProperty("active");
	}

	@Override
	public void setActive(boolean active) {
		setProperty("active", active);
	}

	@Override
	public Release getNextRelease() {
		return out(HAS_NEXT_RELEASE).has(ReleaseImpl.class).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public void setNextRelease(Release release) {
		setUniqueLinkOutTo(release.getImpl(), HAS_NEXT_RELEASE);
	}

	@Override
	public Release getPreviousRelease() {
		return in(HAS_NEXT_RELEASE).has(ReleaseImpl.class).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public ReleaseRoot getRoot() {
		return in(HAS_RELEASE).has(ReleaseRootImpl.class).nextOrDefaultExplicit(ReleaseRootImpl.class, null);
	}

	@Override
	public void assignSchemaVersion(SchemaContainerVersion schemaContainerVersion) {
		setUniqueLinkOutTo(schemaContainerVersion.getImpl(), HAS_VERSION);

		// unlink all other versions
		SchemaContainerVersion previous = schemaContainerVersion.getPreviousVersion();
		while (previous != null) {
			unlinkOut(previous.getImpl(), HAS_VERSION);
			previous = previous.getPreviousVersion();
		}

		SchemaContainerVersion next = schemaContainerVersion.getNextVersion();
		while (next != null) {
			unlinkOut(next.getImpl(), HAS_VERSION);
			next = next.getNextVersion();
		}
	}

	@Override
	public void unassignSchema(SchemaContainer schemaContainer) {
		SchemaContainerVersion version = schemaContainer.getLatestVersion();

		while (version != null) {
			unlinkOut(version.getImpl(), HAS_VERSION);
			version = version.getPreviousVersion();
		}
	}

	@Override
	public boolean contains(SchemaContainer schemaContainer) {
		SchemaContainer foundSchemaContainer = out(HAS_VERSION).has(SchemaContainerVersionImpl.class).in(HAS_PARENT_CONTAINER).has("name", schemaContainer.getName())
				.nextOrDefaultExplicit(SchemaContainerImpl.class, null);
		return foundSchemaContainer != null;
	}

	@Override
	public boolean contains(SchemaContainerVersion schemaContainerVersion) {
		SchemaContainerVersion foundSchemaContainerVersion = out(HAS_VERSION).has(SchemaContainerVersionImpl.class).has("uuid", schemaContainerVersion.getUuid())
				.nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
		return foundSchemaContainerVersion != null;
	}

	@Override
	public SchemaContainerVersion getVersion(SchemaContainer schemaContainer) {
		return out(HAS_VERSION).has(SchemaContainerVersionImpl.class).mark().in(HAS_PARENT_CONTAINER).has("name", schemaContainer.getName()).back()
				.nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	@Override
	public List<? extends SchemaContainerVersion> findAllSchemaVersions()
			throws InvalidArgumentException {
		return out(HAS_VERSION).has(SchemaContainerVersionImpl.class).toListExplicit(SchemaContainerVersionImpl.class);
	}
}
