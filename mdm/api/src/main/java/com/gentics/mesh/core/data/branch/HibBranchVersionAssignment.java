package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.rest.job.JobStatus;

public interface HibBranchVersionAssignment extends HibElement {

	/**
	 * Return the job uuid.
	 * 
	 * @return
	 */
	String getJobUuid();

	/**
	 * Check if the version is active
	 * 
	 * @return
	 */
	boolean isActive();

	/**
	 * Set the branch active flag.
	 * 
	 * @param flag
	 */
	void setActive(boolean flag);

	/**
	 * Return the status for the associated job.
	 * 
	 * 
	 * @return
	 */
	JobStatus getMigrationStatus();

	/**
	 * Set the migration status.
	 * 
	 * @param status
	 */
	void setMigrationStatus(JobStatus status);

}
