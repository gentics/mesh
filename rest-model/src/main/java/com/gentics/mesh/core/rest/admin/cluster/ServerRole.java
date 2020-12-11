package com.gentics.mesh.core.rest.admin.cluster;

/**
 * Roles for clustered Gentics Mesh server instance.
 */
public enum ServerRole {

	/**
	 * Read / Write
	 */
	MASTER,

	/**
	 * Read Only
	 */
	REPLICA;
}
