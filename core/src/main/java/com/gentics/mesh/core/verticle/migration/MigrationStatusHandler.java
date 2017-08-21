package com.gentics.mesh.core.verticle.migration;

import com.gentics.mesh.core.rest.admin.MigrationInfo;
import com.gentics.mesh.core.rest.admin.MigrationType;

/**
 * Interface for migration status of node, release and micronode migrations.
 */
public interface MigrationStatusHandler {

	public static final int MAX_MIGRATION_DATE_ENTRIES = 20;

	public static final String MIGRATION_DATA_MAP_KEY = "mesh.migration.data";

	/**
	 * Return the type of migration which the status is linked to.
	 * 
	 * @return
	 */
	MigrationType getType();

	/**
	 * Update the migration status information
	 * 
	 * @param info
	 * @return Fluent API
	 */
	MigrationStatusHandler updateStatus(MigrationInfo info);

	/**
	 * Update the status and store it in the local or cluster wide map.
	 * 
	 * @return Fluent API
	 */
	MigrationStatusHandler updateStatus();

	/**
	 * Name of the schema.
	 * 
	 * @return schema name
	 */
	String getSourceName();

	/**
	 * Set the name of the migration (eg. name of the schema or release which started it)
	 * 
	 * @param name
	 * @return Fluent API
	 */
	MigrationStatusHandler setSourceName(String name);

	/**
	 * Schema version.
	 * 
	 * @return schema version
	 */
	String getSourceVersion();

	/**
	 * Set the version of the element which started the migration (optional).
	 * 
	 * @param version
	 * @return Fluent API
	 */
	MigrationStatusHandler setSourceVersion(String version);

	/**
	 * Schema version.
	 * 
	 * @return schema version
	 */
	String getTargetVersion();

	/**
	 * Set the version of the target version of the element we are migrating. (optional)
	 * 
	 * @param version
	 * @return Fluent API
	 */
	MigrationStatusHandler setTargetVersion(String version);

	/**
	 * Get total number of elements.
	 * 
	 * @return total number of elements
	 */
	int getTotalElements();

	/**
	 * Get number of migrated elements.
	 * 
	 * @return number of migrated elements
	 */
	int getDoneElements();

	/**
	 * Set the total number of elements to migrate
	 * 
	 * @param totalElements
	 *            total number
	 * @return Fluent API
	 */
	MigrationStatusHandler setTotalElements(int totalElements);

	/**
	 * Increase the number of elements done
	 * 
	 * @return Fluent API
	 */
	MigrationStatusHandler incDoneElements();

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
	MigrationStatusHandler handleError(Throwable error, String string);

	/**
	 * Returns the human readable status.
	 * 
	 * @return
	 */
	String getStatus();

	/**
	 * Return the start time
	 * 
	 * @return
	 */
	Long getStartTime();

	/**
	 * Return the uuid of the source element.
	 * 
	 * @return
	 */
	String getSourceUuid();

	/**
	 * Set the source element uuid.
	 * 
	 * @param sourceUuid
	 * @return Fluent API
	 */
	MigrationStatusHandler setSourceUuid(String sourceUuid);

}
