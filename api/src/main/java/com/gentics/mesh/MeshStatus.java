package com.gentics.mesh;

/**
 * Mesh Status which represents the server state.
 */
public enum MeshStatus {

	/**
	 * Status which indicates that the server is starting up.
	 */
	STARTING,

	/**
	 * Status which indicates that the server is waiting/looking for a cluster to join.
	 */
	WAITING_FOR_CLUSTER,
	/**
	 * Status which indicates that the server is operating normally.
	 */
	READY,

	/**
	 * Status which indicates that the instance is shutting down.
	 */
	SHUTTING_DOWN,

	/**
	 * Status which indicates that a blocking backup is currently running.
	 */
	BACKUP,

	/**
	 * Status which indicates that a blocking restore operation is currently running.
	 */
	RESTORE;
}
