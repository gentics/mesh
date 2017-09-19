package com.gentics.mesh.core.data.release;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;

/**
 * Common class for {@link ReleaseSchemaEdge} and {@link ReleaseMicroschemaEdge} edges
 */
public interface ReleaseVersionEdge {

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

}
