package com.gentics.mesh.core.endpoint.migration;

import com.gentics.mesh.core.data.branch.BranchVersionEdge;
import com.gentics.mesh.core.rest.error.Errors;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.job.JobStatus;

import io.netty.handler.codec.http.HttpResponseStatus;

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
	void setVersionEdge(BranchVersionEdge versionEdge);

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

	default Exception fail(HttpResponseStatus status, String message) {
		GenericRestException ex = Errors.error(status, message);
		error(ex, message);
		throw ex;
	}

}
