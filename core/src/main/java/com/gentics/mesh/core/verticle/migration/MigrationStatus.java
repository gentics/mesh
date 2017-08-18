package com.gentics.mesh.core.verticle.migration;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * Interface for migration status of node, release and micronode migrations.
 */
public interface MigrationStatus {

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
	 */
	void updateStatus(JsonObject info);

	/**
	 * Update the status and store it in the local or cluster wide map.
	 */
	void updateStatus();

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
	 * @return
	 */
	MigrationStatus setSourceName(String name);

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
	 * @return
	 */
	MigrationStatus setSourceVersion(String version);

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
	 * @return
	 */
	MigrationStatus setTargetVersion(String version);

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
	 */
	void setTotalElements(int totalElements);

	/**
	 * Increase the number of elements done
	 */
	void incDoneElements();

	void done(Message<Object> message);

	void handleError(Message<Object> message, Throwable error, String string);

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
	 * @return
	 */
	MigrationStatus setSourceUuid(String sourceUuid);

}
