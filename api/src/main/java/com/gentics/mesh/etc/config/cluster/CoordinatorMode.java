package com.gentics.mesh.etc.config.cluster;

public enum CoordinatorMode {

	/**
	 * Disable delegation.
	 */
	OFF,

	/**
	 * Delegate all requests to the elected master instance.
	 */
	FULL;
}
