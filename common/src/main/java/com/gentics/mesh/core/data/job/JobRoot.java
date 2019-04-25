package com.gentics.mesh.core.data.job;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;

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
	Job enqueueSchemaMigration(User creator, Branch branch, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion);

	/**
	 * Enqueue a branch migration job.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	Job enqueueBranchMigration(User creator, Branch branch, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion);

	/**
	 * Enqueue a microschema migration.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	Job enqueueMicroschemaMigration(User creator, Branch branch, MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion);

	/**
	 * Enqueue a branch migration.
	 * 
	 * @param creator
	 * @param branch
	 * @return
	 */
	Job enqueueBranchMigration(User creator, Branch branch);

	/**
	 * Enqueue a project version purge job.
	 * 
	 * @param user
	 * @param project
	 * @return
	 */
	Job enqueueVersionPurge(User user, Project project);

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
