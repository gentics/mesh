package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_INITIAL_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE_ROOT;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see BranchRoot
 */
public class ReleaseRootImpl extends AbstractRootVertex<Branch> implements BranchRoot {

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
	public Branch create(String name, User creator, String uuid) {
		Branch latestRelease = getLatestRelease();

		Branch branch = getGraph().addFramedVertex(BranchImpl.class);
		if (uuid != null) {
			branch.setUuid(uuid);
		}
		addItem(branch);
		branch.setCreated(creator);
		branch.setName(name);
		branch.setActive(true);
		branch.setMigrated(false);
		branch.setProject(getProject());

		if (latestRelease == null) {
			// if this is the first release, make it the initial release
			setSingleLinkOutTo(branch, HAS_INITIAL_RELEASE);
		} else {
			// otherwise link the releases
			latestRelease.setNextBranch(branch);
		}

		// make the new release the latest
		setSingleLinkOutTo(branch, HAS_LATEST_RELEASE);

		// set initial permissions on the release
		creator.addCRUDPermissionOnRole(getProject(), UPDATE_PERM, branch);

		// assign the newest schema versions of all project schemas to the release
		for (SchemaContainer schemaContainer : getProject().getSchemaContainerRoot().findAllIt()) {
			branch.assignSchemaVersion(creator, schemaContainer.getLatestVersion());
		}

		// ... same for microschemas
		for (MicroschemaContainer microschemaContainer : getProject().getMicroschemaContainerRoot().findAllIt()) {
			branch.assignMicroschemaVersion(creator, microschemaContainer.getLatestVersion());
		}

		return branch;
	}

	@Override
	public Branch getInitialRelease() {
		return out(HAS_INITIAL_RELEASE).nextOrDefaultExplicit(BranchImpl.class, null);
	}

	@Override
	public Branch getLatestRelease() {
		return out(HAS_LATEST_RELEASE).nextOrDefaultExplicit(BranchImpl.class, null);
	}

	@Override
	public Branch create(InternalActionContext ac, SearchQueueBatch batch, String uuid) {
		Database db = MeshInternal.get().database();

		BranchCreateRequest request = ac.fromJson(BranchCreateRequest.class);
		MeshAuthUser requestUser = ac.getUser();

		// Check for completeness of request
		if (StringUtils.isEmpty(request.getName())) {
			throw error(BAD_REQUEST, "release_missing_name");
		}

		Project project = getProject();
		String projectName = project.getName();
		String projectUuid = project.getUuid();

		if (!requestUser.hasPermission(project, GraphPermission.UPDATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", projectUuid + "/" + projectName);
		}

		// Check for uniqueness of release name (per project)
		Branch conflictingRelease = db.checkIndexUniqueness(BranchImpl.UNIQUENAME_INDEX_NAME, BranchImpl.class, getUniqueNameKey(request
			.getName()));
		if (conflictingRelease != null) {
			throw conflict(conflictingRelease.getUuid(), conflictingRelease.getName(), "release_conflicting_name", request.getName());
		}

		Branch branch = create(request.getName(), requestUser, uuid);
		if (!isEmpty(request.getHostname())) {
			branch.setHostname(request.getHostname());
		}
		if (request.getSsl() != null) {
			branch.setSsl(request.getSsl());
		}
		// A new release was created - We also need to create new indices for the nodes within the release
		for (SchemaContainerVersion version : branch.findActiveSchemaVersions()) {
			batch.addNodeIndex(project, branch, version, DRAFT);
			batch.addNodeIndex(project, branch, version, PUBLISHED);
		}
		MeshInternal.get().boot().jobRoot().enqueueBranchMigration(branch.getCreator(), branch);
		return branch;
	}

	@Override
	public Class<? extends Branch> getPersistanceClass() {
		return BranchImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_RELEASE;
	}

	@Override
	public String getUniqueNameKey(String name) {
		return getUuid() + "-" + name;
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting release root {" + getUuid() + "}");
		}

		// Delete all releases
		for (Branch release : findAllIt()) {
			release.delete(batch);
		}

		// All releases are gone. Now delete the root.
		getElement().remove();
	}
}
