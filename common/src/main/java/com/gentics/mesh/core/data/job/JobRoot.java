package com.gentics.mesh.core.data.job;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;

public interface JobRoot extends RootVertex<Job> {

	/**
	 * Enqueue a new job with the given information.
	 * 
	 * @param release
	 * @param fromVersion
	 * @param toVersion
	 * @return Created job
	 */
	Job enqueueSchemaMigration(Release release, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion);

	/**
	 * Enqueue a release migration job.
	 * 
	 * @param release
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	Job enqueueReleaseMigration(Release release, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion);

	/**
	 * Enqueue a microschema migration.
	 * 
	 * @param release
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	Job enqueueMicroschemaMigration(Release release, MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion);

	/**
	 * Enqueue a release migration.
	 * 
	 * @param release
	 * @return
	 */
	Job enqueueReleaseMigration(Release release);

	/**
	 * Process all remaining jobs.
	 */
	void process();

}
