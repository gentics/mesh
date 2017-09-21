package com.gentics.mesh.core.data.release;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;

/**
 * Common class for {@link ReleaseSchemaEdge} and {@link ReleaseMicroschemaEdge} edges
 */
public interface ReleaseVersionEdge {

	public static final String ACTIVE_PROPERTY_KEY = "active";

	public static final String MIGRATION_STATUS_PROPERTY_KEY = "migrationStatus";

	public static final String JOB_UUID_PROPERTY_KEY = "jobUuid";

	/**
	 * Return the migration status for the edge. This will indicate whether the connected schema or microschema has been fully migrated.
	 * 
	 * @return
	 */
	MigrationStatus getMigrationStatus();

	/**
	 * Set the migration status for this edge.
	 * 
	 * @param flag
	 */
	void setMigrationStatus(MigrationStatus status);

	/**
	 * Return the release of this edge.
	 * 
	 * @return
	 */
	Release getRelease();

	/**
	 * Return the referenced job.
	 * 
	 * @return
	 */
	String getJobUuid();

	/**
	 * Set the references job.
	 * 
	 * @param uuid
	 */
	void setJobUuid(String uuid);

	/**
	 * Check whether the version is active and thus should be considered when handling release schema versions.
	 * 
	 * @return
	 */
	boolean isActive();

	/**
	 * Set the active flag of the edge to indicate that the version should no longer be used when considering release schema versions.
	 * 
	 * @param flag
	 */
	void setActive(boolean flag);

}
