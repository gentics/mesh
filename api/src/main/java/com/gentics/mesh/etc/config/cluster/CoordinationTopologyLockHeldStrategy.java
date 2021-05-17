package com.gentics.mesh.etc.config.cluster;

public enum CoordinationTopologyLockHeldStrategy {

	/**
	 * On any DB transaction pass it further to wait until the topology lock is freed.
	 */
	PASS_AND_WAIT_ALL,
	
	/**
	 * On mutating DB transactions fail on held topology lock immediately, and pass the reading DB transactions.
	 */
	DROP_CUD;
}
