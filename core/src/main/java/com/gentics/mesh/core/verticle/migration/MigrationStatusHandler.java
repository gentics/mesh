package com.gentics.mesh.core.verticle.migration;

import com.gentics.mesh.core.data.release.ReleaseVersionEdge;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;

/**
 * Interface for migration status of node, release and micronode migrations.
 */
public interface MigrationStatusHandler {

	/**
	 * Update the status and store it in the local or cluster wide map.
	 * 
	 * @return Fluent API
	 */
	MigrationStatusHandler commitStatus();

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
	void setVersionEdge(ReleaseVersionEdge versionEdge);

	/**
	 * Set the current status.
	 * 
	 * @param status
	 */
	void setStatus(MigrationStatus status);

	/**
	 * Set the current completion count.
	 * 
	 * @param completionCount
	 */
	void setCompletionCount(long completionCount);

	void incCompleted();

}
