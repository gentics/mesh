package com.gentics.mesh.etc.config.cluster;

public enum CoordinatorMode {

	/**
	 * Disable delegation.
	 */
	DISABLED,

	/**
	 * Delegate modifying requests to the elected master instance.
	 */
	MODE_A,

	/**
	 * Delegate all requests to the elected master instance.
	 */
	MODE_B;
}
