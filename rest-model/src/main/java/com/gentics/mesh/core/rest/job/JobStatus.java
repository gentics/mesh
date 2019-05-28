package com.gentics.mesh.core.rest.job;

public enum JobStatus {

	/**
	 * Global status which indicates that no migration is currently running
	 */
	IDLE,

	/**
	 * The migration status has not yet been determined
	 */
	UNKNOWN,

	/**
	 * Migration is currently queued
	 */
	QUEUED,

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
