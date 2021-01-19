package com.gentics.mesh.core.data.job;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.job.JobType;

/**
 * Domain model for job.
 */
public interface HibJob extends HibCoreElement {

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
}
