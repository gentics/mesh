package com.gentics.mesh.core.rest.admin.migration;

public enum MigrationStatus {

	/**
	 * Global status which indicates that no migration is currently running
	 */
	IDLE,

	/**
	 * Migration is currently starting
	 */
	STARTING,
	/**
	 * Migration is currently running
	 */
	RUNNING,

	/**
	 * Migration completed without errors
	 */
	COMPLETED,

	/**
	 * Migration failed with an error
	 */
	FAILED;

}
