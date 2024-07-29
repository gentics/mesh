package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.rest.job.JobStatus;

public interface HibBranchVersionAssignment extends HibElement {

	public static final String ACTIVE_PROPERTY_KEY = "active";

	public static final String MIGRATION_STATUS_PROPERTY_KEY = "migrationStatus";

	public static final String JOB_UUID_PROPERTY_KEY = "jobUuid";

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

	/**
	 * Return the branch of this edge.
	 * 
	 * @return
	 */
	HibBranch getBranch();

	/**
	 * Set the references job.
	 * 
	 * @param uuid
	 */
	void setJobUuid(String uuid);
}
