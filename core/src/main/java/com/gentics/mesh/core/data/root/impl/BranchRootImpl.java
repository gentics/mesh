package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_INITIAL_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_BRANCH;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.apache.commons.lang3.StringUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * @see BranchRoot
 */
public class BranchRootImpl extends AbstractRootVertex<Branch> implements BranchRoot {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(BranchRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_BRANCH));
		index.createIndex(edgeIndex(HAS_BRANCH).withInOut().withOut());
	}

	@Override
	public Project getProject() {
		return in(HAS_BRANCH_ROOT, ProjectImpl.class).next();
	}

	@Override
	public Branch create(String name, User creator, String uuid, boolean setLatest, Branch baseBranch, EventQueueBatch batch) {
		return create(name, creator, uuid, setLatest, baseBranch, true, batch);
	}

	private Branch create(String name, User creator, String uuid, boolean setLatest, Branch baseBranch, boolean assignSchemas,
		EventQueueBatch batch) {
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

		if (baseBranch == null) {
			// if this is the first branch, make it the initial branch
			setSingleLinkOutTo(branch, HAS_INITIAL_BRANCH);
		} else {
			baseBranch.setNextBranch(branch);
		}

		// make the new branch the latest
		if (setLatest || baseBranch == null) {
			setLatestBranch(branch);
		}

		// set initial permissions on the branch
		creator.addCRUDPermissionOnRole(getProject(), UPDATE_PERM, branch);

		if (assignSchemas) {
			assignSchemas(creator, baseBranch, branch, false, batch);
		}

		return branch;
	}

	@Override
	public Branch getInitialBranch() {
		return out(HAS_INITIAL_BRANCH, BranchImpl.class).nextOrNull();
	}

	@Override
	public Branch getLatestBranch() {
		return out(HAS_LATEST_BRANCH, BranchImpl.class).nextOrNull();
	}

	@Override
	public void setLatestBranch(Branch branch) {
		setSingleLinkOutTo(branch, HAS_LATEST_BRANCH);
	}

	@Override
	public Branch create(InternalActionContext ac, EventQueueBatch batch, String uuid) {

		BranchCreateRequest request = ac.fromJson(BranchCreateRequest.class);
		MeshAuthUser requestUser = ac.getUser();

		// Check for completeness of request
		if (StringUtils.isEmpty(request.getName())) {
			throw error(BAD_REQUEST, "branch_missing_name");
		}

		Project project = getProject();
		String projectName = project.getName();
		String projectUuid = project.getUuid();

		if (!requestUser.hasPermission(project, GraphPermission.UPDATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", projectUuid + "/" + projectName, UPDATE_PERM.getRestPerm().getName());
		}

		// Check for uniqueness of branch name (per project)
		Branch conflictingBranch = db().index().checkIndexUniqueness(BranchImpl.UNIQUENAME_INDEX_NAME, BranchImpl.class, getUniqueNameKey(request
			.getName()));
		if (conflictingBranch != null) {
			throw conflict(conflictingBranch.getUuid(), conflictingBranch.getName(), "branch_conflicting_name", request.getName());
		}

		Branch baseBranch = fromReference(request.getBaseBranch());
		if (baseBranch != null && !requestUser.hasPermission(baseBranch, READ_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", baseBranch.getUuid(), READ_PERM.getRestPerm().getName());
		}

		if (baseBranch == null) {
			baseBranch = getLatestBranch();
		}

		Branch branch = create(request.getName(), requestUser, uuid, request.isLatest(), baseBranch, false, batch);
		if (!isEmpty(request.getHostname())) {
			branch.setHostname(request.getHostname());
		}
		if (!isEmpty(request.getPathPrefix())) {
			branch.setPathPrefix(request.getPathPrefix());
		}
		if (request.getSsl() != null) {
			branch.setSsl(request.getSsl());
		}
		User creator = branch.getCreator();
		mesh().boot().jobRoot().enqueueBranchMigration(creator, branch);
		assignSchemas(creator, baseBranch, branch, true, batch);

		batch.add(branch.onCreated());

		return branch;
	}

	/**
	 * Assigns schemas and microschemas to the new branch, which will cause a node migration if there is a newer schema version.
	 *
	 * @param creator
	 *            The creator of the branch
	 * @param baseBranch
	 *            The branch which the new branch is based on
	 * @param newBranch
	 *            The newly created branch
	 * @param batch
	 */
	private void assignSchemas(User creator, Branch baseBranch, Branch newBranch, boolean migrate, EventQueueBatch batch) {
		// Assign the same schema versions as the base branch, so that a migration can be started
		if (baseBranch != null && migrate) {
			for (SchemaContainerVersion schemaContainerVersion : baseBranch.findActiveSchemaVersions()) {
				newBranch.assignSchemaVersion(creator, schemaContainerVersion, batch);
			}
		}

		// assign the newest schema versions of all project schemas to the branch
		for (SchemaContainer schemaContainer : getProject().getSchemaContainerRoot().findAll()) {
			newBranch.assignSchemaVersion(newBranch.getCreator(), schemaContainer.getLatestVersion(), batch);
		}

		// ... same for microschemas
		if (baseBranch != null && migrate) {
			for (MicroschemaContainerVersion microschemaContainerVersion : baseBranch.findActiveMicroschemaVersions()) {
				newBranch.assignMicroschemaVersion(creator, microschemaContainerVersion, batch);
			}
		}

		for (MicroschemaContainer microschemaContainer : getProject().getMicroschemaContainerRoot().findAll()) {
			newBranch.assignMicroschemaVersion(newBranch.getCreator(), microschemaContainer.getLatestVersion(), batch);
		}
	}

	@Override
	public Class<? extends Branch> getPersistanceClass() {
		return BranchImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_BRANCH;
	}

	@Override
	public String getUniqueNameKey(String name) {
		return getUuid() + "-" + name;
	}

	@Override
	public void delete(BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting branch root {" + getUuid() + "}");
		}

		// Delete all branches
		for (Branch branch : findAll()) {
			branch.delete(bac);
			bac.process();
		}

		// All branches are gone. Now delete the root.
		getElement().remove();
		bac.process();
	}

	@Override
	public Branch fromReference(BranchReference reference) {
		Branch branch = null;

		if (reference == null) {
			return null;
		}

		if (!reference.isSet()) {
			return null;
		}

		if (!StringUtils.isEmpty(reference.getUuid())) {
			branch = findByUuid(reference.getUuid());
			if (branch == null) {
				throw error(NOT_FOUND, "object_not_found_for_uuid", reference.getUuid());
			}
		} else if (!StringUtils.isEmpty(reference.getName())) {
			branch = findByName(reference.getName());
			if (branch == null) {
				throw error(NOT_FOUND, "object_not_found_for_name", reference.getName());
			}
		}

		return branch;
	}
}
