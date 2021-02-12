package com.gentics.mesh.core.data.job;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibTransformableElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;

public interface HibJob extends HibCoreElement {

	/**
	 * Set the branch reference for this job.
	 * @param branch
	 */
	void setBranch(HibBranch branch);

	/**
	 * Mark the error as failed and store the exception information.
	 * 
	 * @param ex
	 */
	void markAsFailed(Exception ex);

	/**
	 * Check whether the job has failed.
	 *
	 * @return
	 */
	boolean hasFailed();

	/**
	 * Return the detailed error report.
	 * 
	 * @return
	 */
	String getErrorDetail();

	/**
	 * Return the branch of the job.
	 * 
	 * @return
	 */
	HibBranch getBranch();

	/**
	 * Return the creator of the job.
	 * 
	 * @return
	 */
	HibUser getCreator();

	/**
	 * Return the type of the job.
	 * 
	 * @return
	 */
	JobType getType();

	/**
	 * Return the job error message.
	 * 
	 * @return
	 */
	String getErrorMessage();

	/**
	 * Remove the job.
	 */
	void remove();

	/**
	 * Removes the error information from the job and thus it can be processed again.
	 */
	void resetJob();

	/**
	 * Get migration status.
	 *
	 * @return
	 */
	JobStatus getStatus();

	/**
	 * Set migration status.
	 *
	 * @param status
	 */
	void setStatus(JobStatus status);
}
