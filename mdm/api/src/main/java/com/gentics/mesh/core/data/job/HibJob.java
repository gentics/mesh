package com.gentics.mesh.core.data.job;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;

/**
 * Domain model for job.
 */
public interface HibJob extends HibCoreElement<JobResponse>, HibCreatorTracking {

	/**
	 * Set the branch reference for this job.
	 * 
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
	 * Return the human readable error message.
	 * 
	 * @return
	 */
	String getErrorMessage();

	/**
	 * Set the human readable error message.
	 * 
	 * @param message
	 */
	void setErrorMessage(String message);

	/**
	 * Return the error detail information.
	 * 
	 * @return
	 */
	String getErrorDetail();

	/**
	 * Set the error detail information.
	 * 
	 * @param info
	 */
	void setErrorDetail(String info);

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

	/**
	 * Removes the error information from the job and thus it can be processed again.
	 */
	void resetJob();

	/**
	 * Check whether the job has failed.
	 * 
	 * @return
	 */
	boolean hasFailed();

	/**
	 * Remove the job.
	 */
	void remove();
}
