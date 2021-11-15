package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.QUEUED;
import static com.gentics.mesh.event.Assignment.ASSIGNED;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchMicroschemaVersion;
import com.gentics.mesh.core.data.branch.HibBranchSchemaVersion;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A persisting extension to {@link BranchDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingBranchDao extends BranchDao, PersistingRootDao<HibProject, HibBranch> {

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
	default boolean update(HibBranch branch, InternalActionContext ac, EventQueueBatch batch) {
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
	default void delete(HibProject project, HibBranch branch, BulkActionContext bac) {
		bac.add(branch.onDeleted());
		deletePersisted(project, branch);
	}

	@Override
	default HibJob assignSchemaVersion(HibBranch branch, HibUser user, HibSchemaVersion schemaVersion, EventQueueBatch batch) {
		JobDao jobDao = Tx.get().jobDao();
		HibBranchSchemaVersion edge = branch.findBranchSchemaEdge(schemaVersion);
		HibJob job = null;
		// Don't remove any existing edge. Otherwise the edge properties are lost
		if (edge == null) {
			HibSchemaVersion currentVersion = branch.findLatestSchemaVersion(schemaVersion.getSchemaContainer());
			edge = connectToSchemaVersion(branch, currentVersion);
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
			batch.add(branch.onSchemaAssignEvent(schemaVersion, ASSIGNED, edge.getMigrationStatus()));
		}
		return job;
	}

	@Override
	default HibJob assignMicroschemaVersion(HibBranch branch, HibUser user, HibMicroschemaVersion microschemaVersion, EventQueueBatch batch) {
		JobDao jobDao = Tx.get().jobDao();
		HibBranchMicroschemaVersion edge = branch.findBranchMicroschemaEdge(microschemaVersion);
		HibJob job = null;
		// Don't remove any existing edge. Otherwise the edge properties are lost
		if (edge == null) {
			HibMicroschemaVersion currentVersion = branch.findLatestMicroschemaVersion(microschemaVersion.getSchemaContainer());
			edge = connectToMicroschemaVersion(branch, currentVersion);
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
			batch.add(branch.onMicroschemaAssignEvent(microschemaVersion, ASSIGNED, edge.getMigrationStatus()));
		}
		return job;
	}
}
