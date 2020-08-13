package com.gentics.mesh.core.data.job;

import java.time.ZonedDateTime;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
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
	Job enqueueSchemaMigration(HibUser creator, HibBranch branch, SchemaVersion fromVersion, SchemaVersion toVersion);

	/**
	 * Enqueue a branch migration job.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	Job enqueueBranchMigration(HibUser creator, HibBranch branch, SchemaVersion fromVersion, SchemaVersion toVersion);

	/**
	 * Enqueue a microschema migration.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	Job enqueueMicroschemaMigration(HibUser creator, HibBranch branch, MicroschemaVersion fromVersion, MicroschemaVersion toVersion);

	/**
	 * Enqueue a branch migration.
	 * 
	 * @param creator
	 * @param branch
	 * @return
	 */
	Job enqueueBranchMigration(HibUser creator, HibBranch branch);

	/**
	 * Enqueue a project version purge job that is limited to the given date.
	 * 
	 * @param user
	 * @param project
	 * @param before
	 * @return
	 */
	Job enqueueVersionPurge(HibUser user, HibProject project, ZonedDateTime before);

	/**
	 * Enqueue a project version purge job.
	 * @param user
	 * @param project
	 * @return
	 */
	Job enqueueVersionPurge(HibUser user, HibProject project);

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
