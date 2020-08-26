package com.gentics.mesh.core.data.job;

import java.time.ZonedDateTime;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;

import io.reactivex.Completable;

/**
 * Aggregation vertex for jobs.
 */
public interface JobRoot extends RootVertex<Job> {

	/**
	 * Enqueue a new job with the given information.
	 *
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return Created job
	 */
	HibJob enqueueSchemaMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion, HibSchemaVersion toVersion);

	/**
	 * Enqueue a branch migration job.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	HibJob enqueueBranchMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion, HibSchemaVersion toVersion);

	/**
	 * Enqueue a microschema migration.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	HibJob enqueueMicroschemaMigration(HibUser creator, HibBranch branch, HibMicroschemaVersion fromVersion, HibMicroschemaVersion toVersion);

	/**
	 * Enqueue a branch migration.
	 * 
	 * @param creator
	 * @param branch
	 * @return
	 */
	HibJob enqueueBranchMigration(HibUser creator, HibBranch branch);

	/**
	 * Enqueue a project version purge job that is limited to the given date.
	 * 
	 * @param user
	 * @param project
	 * @param before
	 * @return
	 */
	HibJob enqueueVersionPurge(HibUser user, HibProject project, ZonedDateTime before);

	/**
	 * Enqueue a project version purge job.
	 * @param user
	 * @param project
	 * @return
	 */
	HibJob enqueueVersionPurge(HibUser user, HibProject project);

	/**
	 * Process all remaining jobs.
	 */
	Completable process();

	/**
	 * Purge all failed jobs from the job root.
	 */
	void purgeFailed();

	/**
	 * Delete all jobs.
	 */
	void clear();
}
