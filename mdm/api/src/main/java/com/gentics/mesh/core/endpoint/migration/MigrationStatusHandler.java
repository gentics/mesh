package com.gentics.mesh.core.endpoint.migration;

import com.gentics.mesh.core.data.branch.BranchVersionAssignment;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobWarningListModel;


/**
 * Interface for migration status of node, branch and micronode migrations.
 */
public interface MigrationStatusHandler {

	/**
	 * Update the status and store it in the local or cluster wide map.
	 *
	 * @return Fluent API
	 */
	MigrationStatusHandler commit();

	/**
	 * Update status and inform all the channels.
	 *
	 * @return Fluent API
	 */

	MigrationStatusHandler done();

	/**
	 * Update status and inform all the channels, setting the provided warning list
	 * @param warningList
	 * @return
	 */
	MigrationStatusHandler done(JobWarningListModel warningList);

	/**
	 * Handle the error and inform all channels.
	 *
	 * @param error
	 * @param string
	 * @return Fluent API
	 */
	MigrationStatusHandler error(Throwable error, String string);

	/**
	 * Set the version edge which will store the migration status.
	 * 
	 * @param versionEdge
	 */
	void setVersionEdge(BranchVersionAssignment versionEdge);

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
	void incCompleted(int increment);
}
