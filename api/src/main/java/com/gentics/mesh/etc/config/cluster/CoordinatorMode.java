package com.gentics.mesh.etc.config.cluster;

public enum CoordinatorMode {

	/**
	 * Disable delegation.
	 */
	DISABLED,

	/**
	 * Delegate modifying (Create, Update, Delete) requests to the elected master instance.
	 */
	CUD,

	/**
	 * Delegate all requests to the elected master instance.
	 */
	ALL;
}
