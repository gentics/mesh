package com.gentics.mesh.core.data.job;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.gentics.mesh.core.data.project.HibProject;

/**
 * Extension of {@link HibJob} for version purge jobs.
 */
public interface HibVersionPurgeJob extends HibJob {

	/**
	 * Get the project reference of the job.
	 * @return
	 */
	HibProject getProject();

	/**
	 * Set the project reference for the job.
	 *
	 * @param project
	 */
	void setProject(HibProject project);

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
