package com.gentics.mesh.core.data.job;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;

/**
 * Aggregation vertex for jobs.
 */
public interface JobRoot extends RootVertex<Job> {

	/**
	 * Enqueue a new job with the given information.
	 *
	 * @param creator
	 * @param release
	 * @param fromVersion
	 * @param toVersion
	 * @return Created job
	 */
	Job enqueueSchemaMigration(User creator, Release release, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion);

	/**
	 * Enqueue a release migration job.
	 * 
	 * @param creator
	 * @param release
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	Job enqueueReleaseMigration(User creator, Release release, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion);

	/**
	 * Enqueue a microschema migration.
	 * 
	 * @param creator
	 * @param release
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	Job enqueueMicroschemaMigration(User creator, Release release, MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion);

	/**
	 * Enqueue a release migration.
	 * 
	 * @param creator
	 * @param release
	 * @return
	 */
	Job enqueueReleaseMigration(User creator, Release release);

	/**
	 * Process all remaining jobs.
	 */
	void process();

	/**
	 * Purge all failed jobs from the job root.
	 */
	void purgeFailed();

	/**
	 * Delete all jobs.
	 */
	void clear();
}
