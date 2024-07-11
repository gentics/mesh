package com.gentics.mesh.core.data.job;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.gentics.mesh.core.data.project.Project;

/**
 * Extension of {@link Job} for version purge jobs.
 */
public interface VersionPurgeJob extends Job {

	/**
	 * Get the project reference of the job.
	 * @return
	 */
	Project getProject();

	/**
	 * Set the project reference for the job.
	 *
	 * @param project
	 */
	void setProject(Project project);

	/**
	 * Return the max age setting for the purge operation.
	 *
	 * @return
	 */
	Optional<ZonedDateTime> getMaxAge();

	/***
	 * Set the max age setting for the purge operation.
	 *
	 * @param time
	 */
	void setMaxAge(ZonedDateTime time);
}
