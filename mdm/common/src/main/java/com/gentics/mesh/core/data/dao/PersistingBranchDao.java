package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.QUEUED;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cache.NameCache;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchMicroschemaVersion;
import com.gentics.mesh.core.data.branch.HibBranchSchemaVersion;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A persisting extension to {@link BranchDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingBranchDao extends BranchDao, PersistingRootDao<HibProject, HibBranch>, PersistingNamedEntityDao<HibBranch> {
	static final Logger log = LoggerFactory.getLogger(BranchDao.class);

	/**
	 * Make a new connection of the branch to the schema version, containing the migration status data.
	 * 
	 * @param branch
	 * @param version
	 * @return
	 */
	HibBranchSchemaVersion connectToSchemaVersion(HibBranch branch, HibSchemaVersion version);

	/**
	 * Make a new connection of the branch to the microschema version, containing the migration status data.
	 * 
	 * @param branch
	 * @param version
	 * @return
	 */
	HibBranchMicroschemaVersion connectToMicroschemaVersion(HibBranch branch, HibMicroschemaVersion version);

	@Override
	default HibBranch create(HibProject project, String name, HibUser creator, String uuid, boolean setLatest, HibBranch baseBranch, EventQueueBatch batch) {
		return create(project, name, creator, uuid, setLatest, baseBranch, true, batch);
	}

	/**
	 * Create a new branch and make it the latest The new branch will be the initial branch, if it is the first created.
	 *
	 * @param name
	 *            branch name
	 * @param creator
	 *            creator
	 * @param batch
	 * @return new Branch
	 */
	default HibBranch create(HibProject project, String name, HibUser creator, EventQueueBatch batch) {
		return create(project, name, creator, null, true, getLatestBranch(project), batch);
	}

	/**
	 * Create the branch element with the specified parameters.
	 * 
	 * @param name
	 * @param creator
	 * @param uuid
	 * @param setLatest
	 * @param baseBranch
	 * @param assignSchemas
	 * @param batch
	 * @return
	 */
	default HibBranch create(HibProject project, String name, HibUser creator, String uuid, boolean setLatest, HibBranch baseBranch, boolean assignSchemas,
		EventQueueBatch batch) {
		HibBranch branch = createPersisted(project, uuid, b -> {
			b.setCreated(creator);
			b.setName(name);
			b.setActive(true);
			b.setMigrated(false);
		});

		if (baseBranch == null) {
			// if this is the first branch, make it the initial branch
			branch.setInitial();
		} else {
			branch.setPreviousBranch(baseBranch);
		}

		// make the new branch the latest
		if (setLatest || baseBranch == null) {
			branch.setLatest();
		}

		UserDao userRoot = Tx.get().userDao();

		// set initial permissions on the branch
		userRoot.inheritRolePermissions(creator, project, branch);

		if (assignSchemas) {
			assignSchemas(project, creator, baseBranch, branch, false, batch);
		}

		mergeIntoPersisted(project, branch);

		batch.add(branch.onCreated());
		return branch;
	}

	@Override
	default HibBranch create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		CommonTx tx = CommonTx.get();
		BranchCreateRequest request = ac.fromJson(BranchCreateRequest.class);
		HibUser requestUser = ac.getUser();

		// Check for completeness of request
		if (StringUtils.isEmpty(request.getName())) {
			throw error(BAD_REQUEST, "branch_missing_name");
		}

		String projectName = project.getName();
		String projectUuid = project.getUuid();

		UserDao userDao = tx.userDao();

		if (!userDao.hasPermission(requestUser, project, InternalPermission.UPDATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", projectUuid + "/" + projectName, UPDATE_PERM.getRestPerm().getName());
		}

		// Check for uniqueness of branch name (per project)
		HibBranch conflictingBranch = findByName(project, request.getName());
		if (conflictingBranch != null) {
			throw conflict(conflictingBranch.getUuid(), conflictingBranch.getName(), "branch_conflicting_name", request.getName());
		}

		HibBranch baseBranch = fromReference(project, request.getBaseBranch());
		if (baseBranch != null && !userDao.hasPermission(requestUser, baseBranch, READ_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", baseBranch.getUuid(), READ_PERM.getRestPerm().getName());
		}

		if (baseBranch == null) {
			baseBranch = getLatestBranch(project);
		}

		HibBranch branch = create(project, request.getName(), requestUser, uuid, request.isLatest(), baseBranch, false, batch);
		if (!isEmpty(request.getHostname())) {
			branch.setHostname(request.getHostname());
		}
		if (!isEmpty(request.getPathPrefix())) {
			branch.setPathPrefix(request.getPathPrefix());
		}
		if (request.getSsl() != null) {
			branch.setSsl(request.getSsl());
		}
		HibUser creator = branch.getCreator();
		tx.jobDao().enqueueBranchMigration(creator, branch);
		assignSchemas(project, creator, baseBranch, branch, true, batch);

		batch.add(() -> MeshEvent.triggerJobWorker(tx.data().mesh().vertx().eventBus(), tx.data().options()));

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
	private void assignSchemas(HibProject project, HibUser creator, HibBranch baseBranch, HibBranch newBranch, boolean migrate, EventQueueBatch batch) {
		// Assign the same schema versions as the base branch, so that a migration can be started
		if (baseBranch != null && migrate) {
			for (HibSchemaVersion schemaVersion : baseBranch.findActiveSchemaVersions()) {
				assignSchemaVersion(newBranch, creator, schemaVersion, batch);
			}
		}

		// assign the newest schema versions of all project schemas to the branch
		for (HibSchema schemaContainer : project.getSchemas()) {
			assignSchemaVersion(newBranch, newBranch.getCreator(), schemaContainer.getLatestVersion(), batch);
		}

		// ... same for microschemas
		if (baseBranch != null && migrate) {
			for (HibMicroschemaVersion microschemaVersion : baseBranch.findActiveMicroschemaVersions()) {
				assignMicroschemaVersion(newBranch, creator, microschemaVersion, batch);
			}
		}

		for (HibMicroschema microschema : project.getMicroschemas()) {
			assignMicroschemaVersion(newBranch, newBranch.getCreator(), microschema.getLatestVersion(), batch);
		}
	}

	@Override
	default void onRootDeleted(HibProject project) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting branches of project {" + project.getUuid() + "}");
		}

		// Delete all branches
		for (HibBranch branch : findAll(project).list()) {
			CommonTx.get().batch().add(branch.onDeleted());
			deletePersisted(project, branch);
			CommonTx.get().data().maybeGetBulkActionContext().ifPresent(BulkActionContext::process);
		}
	}

	default HibBranch fromReference(HibProject project, BranchReference reference) {
		HibBranch branch = null;

		if (reference == null) {
			return null;
		}

		if (!reference.isSet()) {
			return null;
		}

		if (!StringUtils.isEmpty(reference.getUuid())) {
			branch = findByUuid(project, reference.getUuid());
			if (branch == null) {
				throw error(NOT_FOUND, "object_not_found_for_uuid", reference.getUuid());
			}
		} else if (!StringUtils.isEmpty(reference.getName())) {
			branch = findByName(project, reference.getName());
			if (branch == null) {
				throw error(NOT_FOUND, "object_not_found_for_name", reference.getName());
			}
		}

		return branch;
	}

	@Override
	default BranchResponse transformToRestSync(HibBranch branch, InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		BranchResponse restBranch = new BranchResponse();
		if (fields.has("name")) {
			restBranch.setName(branch.getName());
		}
		if (fields.has("hostname")) {
			restBranch.setHostname(branch.getHostname());
		}
		if (fields.has("ssl")) {
			restBranch.setSsl(branch.getSsl());
		}
		// restRelease.setActive(isActive());
		if (fields.has("migrated")) {
			restBranch.setMigrated(branch.isMigrated());
		}
		if (fields.has("tags")) {
			setTagsToRest(branch, ac, restBranch);
		}
		if (fields.has("latest")) {
			restBranch.setLatest(branch.isLatest());
		}
		if (fields.has("pathPrefix")) {
			restBranch.setPathPrefix(branch.getPathPrefix());
		}

		// Add common fields
		branch.fillCommonRestFields(ac, fields, restBranch);

		// Role permissions
		Tx.get().roleDao().setRolePermissions(branch, ac, restBranch);
		return restBranch;
	}

	@Override
	default boolean update(HibProject project, HibBranch branch, InternalActionContext ac, EventQueueBatch batch) {
		// Don't update the branch, if it does not belong to the requested project.
		if (!project.getUuid().equals(branch.getProject().getUuid())) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", branch.getUuid());
		}

		BranchUpdateRequest requestModel = ac.fromJson(BranchUpdateRequest.class);
		boolean modified = false;

		if (shouldUpdate(requestModel.getName(), branch.getName())) {
			// Check for conflicting project name
			HibBranch conflictingBranch = findConflictingBranch(branch, requestModel.getName());
			if (conflictingBranch != null) {
				throw conflict(conflictingBranch.getUuid(), conflictingBranch.getName(), "branch_conflicting_name", requestModel.getName());
			}
			branch.setName(requestModel.getName());
			modified = true;
		}

		if (shouldUpdate(requestModel.getHostname(), branch.getHostname())) {
			branch.setHostname(requestModel.getHostname());
			modified = true;
		}

		if (shouldUpdate(requestModel.getPathPrefix(), branch.getPathPrefix())) {
			branch.setPathPrefix(requestModel.getPathPrefix());
			modified = true;
		}

		if (requestModel.getSsl() != null && requestModel.getSsl() != branch.getSsl()) {
			branch.setSsl(requestModel.getSsl());
			modified = true;
		}

		if (modified) {
			branch.setEditor(ac.getUser());
			branch.setLastEditedTimestamp();
			batch.add(branch.onUpdated());
		}
		return modified;
	}

	@Override
	default void delete(HibProject project, HibBranch branch) {
		CommonTx.get().batch().add(branch.onDeleted());
		deletePersisted(project, branch);
	}

	@Override
	default HibJob assignSchemaVersion(HibBranch branch, HibUser user, HibSchemaVersion schemaVersion, EventQueueBatch batch) {
		JobDao jobDao = Tx.get().jobDao();
		HibBranchSchemaVersion edge = findBranchSchemaEdge(branch, schemaVersion);
		HibJob job = null;
		// Don't remove any existing edge. Otherwise the edge properties are lost
		if (edge == null) {
			HibSchemaVersion currentVersion = branch.findLatestSchemaVersion(schemaVersion.getSchemaContainer());
			edge = connectToSchemaVersion(branch, schemaVersion);
			// Enqueue the schema migration for each found schema version
			edge.setActive(true);
			if (currentVersion != null) {
				job = jobDao.enqueueSchemaMigration(user, branch, currentVersion, schemaVersion);
				edge.setMigrationStatus(QUEUED);
				edge.setJobUuid(job.getUuid());
			} else {
				// No migration needed since there was no previous version assigned.
				edge.setMigrationStatus(COMPLETED);
			}
			batch.add(branch.onSchemaAssignEvent(schemaVersion, ASSIGNED, edge.getMigrationStatus(), currentVersion));
		}
		return job;
	}

	@Override
	default HibJob assignMicroschemaVersion(HibBranch branch, HibUser user, HibMicroschemaVersion microschemaVersion, EventQueueBatch batch) {
		JobDao jobDao = Tx.get().jobDao();
		HibBranchMicroschemaVersion edge = findBranchMicroschemaEdge(branch, microschemaVersion);
		HibJob job = null;
		// Don't remove any existing edge. Otherwise the edge properties are lost
		if (edge == null) {
			HibMicroschemaVersion currentVersion = branch.findLatestMicroschemaVersion(microschemaVersion.getSchemaContainer());
			edge = connectToMicroschemaVersion(branch, microschemaVersion);
			// Enqueue the job so that the worker can process it later on
			edge.setActive(true);
			if (currentVersion != null) {
				job = jobDao.enqueueMicroschemaMigration(user, branch, currentVersion, microschemaVersion);
				edge.setMigrationStatus(QUEUED);
				edge.setJobUuid(job.getUuid());
			} else {
				// No migration needed since there was no previous version assigned.
				edge.setMigrationStatus(COMPLETED);
			}
			batch.add(branch.onMicroschemaAssignEvent(microschemaVersion, ASSIGNED, edge.getMigrationStatus(), currentVersion));
		}
		return job;
	}

	/**
	 * Set the tag information to the rest model.
	 * 
	 * @param branch
	 * @param ac
	 * @param restNode
	 *            Rest model which will be updated
	 */
	private void setTagsToRest(HibBranch branch, InternalActionContext ac, BranchResponse restNode) {
		restNode.setTags(branch.getTags().stream().map(HibTag::transformToReference).collect(Collectors.toList()));
	}

	/**
	 * Look for a branch in the project by either UUID or name, making aware of the cache.
	 * 
	 * @param project
	 * @param branchNameOrUuid
	 * @return
	 */
	default Optional<HibBranch> findBranchOpt(HibProject project, String branchNameOrUuid) {
		return Optional.ofNullable(CommonTx.get().data().mesh().branchCache().get(getCacheKey(project, branchNameOrUuid), key -> {
			HibBranch branch = null;

			if (!isEmpty(branchNameOrUuid)) {
				branch = findByUuid(project, branchNameOrUuid);
				if (branch == null) {
					branch = findByName(project, branchNameOrUuid);
				}
				if (branch == null) {
					return null;
				}
			} else {
				branch = getLatestBranch(project);
			}

			return branch;
		}));
	}

	@Override
	default HibBranch findBranch(HibProject project, String branchNameOrUuid) {
		return findBranchOpt(project, branchNameOrUuid)
			.orElseThrow(() -> error(BAD_REQUEST, "branch_error_not_found", branchNameOrUuid));
	}

	@Override
	default HibBranch findBranchOrLatest(HibProject project, String branchNameOrUuid) {
		return findBranchOpt(project, branchNameOrUuid).orElseGet(() -> getLatestBranch(project));
	}

	@Override
	default Optional<NameCache<HibBranch>> maybeGetCache() {
		return Tx.maybeGet().map(CommonTx.class::cast).map(tx -> tx.data().mesh().branchCache());
	}
}
