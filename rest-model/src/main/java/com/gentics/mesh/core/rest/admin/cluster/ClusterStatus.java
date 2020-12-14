package com.gentics.mesh.core.rest.admin.cluster;

/**
 * Gentics Mesh cluster instance status.
 */
public enum ClusterStatus {

	/**
	 * The instance is online
	 */
	ONLINE,

	/**
	 * The instance is currently syncing
	 */
	SYNCHRONIZING,

	/**
	 * The instance is currently joining the cluster
	 */
	JOINING,

	/**
	 * The instance is shutting down
	 */
	SHUTTING_DOWN;
}
