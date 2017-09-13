package com.gentics.mesh.core.verticle.migration;

import com.gentics.mesh.core.data.release.ReleaseVersionEdge;
import com.gentics.mesh.core.rest.admin.migration.MigrationInfo;

/**
 * Interface for migration status of node, release and micronode migrations.
 */
public interface MigrationStatusHandler {

	public static final int MAX_MIGRATION_INFO_ENTRIES = 20;

	public static final String MIGRATION_DATA_MAP_KEY = "mesh.migration.data";

	/**
	 * Update the status and store it in the local or cluster wide map.
	 * 
	 * @return Fluent API
	 */
	MigrationStatusHandler updateStatus();

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
	 * Return the migration info object which contains the current state of the migration.
	 * 
	 * @return
	 */
	MigrationInfo getInfo();

	/**
	 * Set the version edge which will store the migration status.
	 * @param versionEdge
	 */
	void setVersionEdge(ReleaseVersionEdge versionEdge);

}
