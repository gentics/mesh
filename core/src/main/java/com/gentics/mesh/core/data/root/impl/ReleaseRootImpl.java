package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_INITIAL_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE_ROOT;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

public class ReleaseRootImpl extends AbstractRootVertex<Release> implements ReleaseRoot {

	public static void init(Database database) {
		database.addVertexType(ReleaseRootImpl.class, MeshVertexImpl.class);
		database.addEdgeType(HAS_RELEASE);
		database.addEdgeIndex(HAS_RELEASE, true, false, true);
	}

	@Override
	public Project getProject() {
		return in(HAS_RELEASE_ROOT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public Release create(String name, User creator) {
		Release latestRelease = getLatestRelease();

		Release release = getGraph().addFramedVertex(ReleaseImpl.class);
		addItem(release);
		release.setCreated(creator);
		release.setName(name);
		release.setActive(true);
		release.setMigrated(false);
		release.setProject(getProject());

		if (latestRelease == null) {
			// if this is the first release, make it the initial release
			setSingleLinkOutTo(release.getImpl(), HAS_INITIAL_RELEASE);
		} else {
			// otherwise link the releases
			latestRelease.setNextRelease(release);
		}

		// make the new release the latest
		setSingleLinkOutTo(release.getImpl(), HAS_LATEST_RELEASE);

		// set initial permissions on the release
		creator.addCRUDPermissionOnRole(getProject(), UPDATE_PERM, release);

		// assign the newest schema versions of all project schemas to the release
		List<? extends SchemaContainer> projectSchemas = getProject().getSchemaContainerRoot().findAll();
		for (SchemaContainer schemaContainer : projectSchemas) {
			release.assignSchemaVersion(schemaContainer.getLatestVersion());
		}

		// ... same for microschemas
		List<? extends MicroschemaContainer> projectMicroschemas = getProject().getMicroschemaContainerRoot().findAll();
		for (MicroschemaContainer microschemaContainer : projectMicroschemas) {
			release.assignMicroschemaVersion(microschemaContainer.getLatestVersion());
		}

		return release;
	}

	@Override
	public Release getInitialRelease() {
		return out(HAS_INITIAL_RELEASE).has(ReleaseImpl.class).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public Release getLatestRelease() {
		return out(HAS_LATEST_RELEASE).has(ReleaseImpl.class).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public Release create(InternalActionContext ac, SearchQueueBatch batch) {
		Database db = MeshInternal.get().database();

		ReleaseCreateRequest createRequest = ac.fromJson(ReleaseCreateRequest.class);
		MeshAuthUser requestUser = ac.getUser();

		// check for completeness of request
		if (StringUtils.isEmpty(createRequest.getName())) {
			throw error(BAD_REQUEST, "release_missing_name");
		}

		Project project = getProject();
		String projectName = project.getName();
		String projectUuid = project.getUuid();

		if (!requestUser.hasPermission(project, GraphPermission.UPDATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", projectUuid + "/" + projectName);
		}

		requestUser.reload();

		// Check for uniqueness of release name (per project)
		Release conflictingRelease = db.checkIndexUniqueness(ReleaseImpl.UNIQUENAME_INDEX_NAME, ReleaseImpl.class,
				getUniqueNameKey(createRequest.getName()));
		if (conflictingRelease != null) {
			throw conflict(conflictingRelease.getUuid(), conflictingRelease.getName(), "release_conflicting_name", createRequest.getName());
		}

		Release release = create(createRequest.getName(), requestUser);
		NodeIndexHandler.getIndexName(project.getUuid(), release.getUuid(), "draft");

		// Create index queue entries for creating indices
		batch.addEntry(NodeIndexHandler.getIndexName(project.getUuid(), release.getUuid(), "draft"), Node.TYPE, SearchQueueEntryAction.CREATE_INDEX);
		batch.addEntry(NodeIndexHandler.getIndexName(project.getUuid(), release.getUuid(), "published"), Node.TYPE,
				SearchQueueEntryAction.CREATE_INDEX);
		return release;
	}

	@Override
	public Class<? extends Release> getPersistanceClass() {
		return ReleaseImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_RELEASE;
	}

	@Override
	public String getUniqueNameKey(String name) {
		return getUuid() + "-" + name;
	}
}
