package com.gentics.mesh.core.data.release;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;

/**
 * Common class for schema and microschema edge methods.
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

}
