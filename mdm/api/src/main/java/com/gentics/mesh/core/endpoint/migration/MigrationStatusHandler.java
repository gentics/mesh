package com.gentics.mesh.core.endpoint.migration;

import com.gentics.mesh.core.data.branch.HibBranchVersionAssignment;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.rest.job.JobStatus;


/**
 * Interface for migration status of node, branch and micronode migrations.
 */
public interface MigrationStatusHandler {

	/**
	 * Update the status and store it in the local or cluster wide map.
	 *
	 * @param job
	 *
	 * @return Fluent API
	 */
	MigrationStatusHandler commit(HibJob job);

	/**
	 * Update status and inform all the channels.
	 *
	 * @param job
	 * @return Fluent API
	 */

	MigrationStatusHandler done(HibJob job);

	/**
	 * Handle the error and inform all channels.
	 *
	 * @param job
	 * @param error
	 * @param string
	 * @return Fluent API
	 */
	MigrationStatusHandler error(HibJob job, Throwable error, String string);

	/**
	 * Set the version edge which will store the migration status.
	 * 
	 * @param versionEdge
	 */
	void setVersionEdge(HibBranchVersionAssignment versionEdge);

	/**
	 * Set the current status.
	 * 
	 * @param status
	 */
	void setStatus(JobStatus status);

	/**
	 * Set the current completion count.
	 * 
	 * @param completionCount
	 */
	void setCompletionCount(long completionCount);

	/**
	 * Increment the completion counter.
	 */
	void incCompleted();
}
