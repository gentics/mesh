package com.gentics.mesh.core.rest.admin.consistency;

/**
 * Rating for consistency checks
 */
public enum ConsistencyRating {

	/**
	 * Check passed
	 */
	CONSISTENT,

	/**
	 * Check did not pass and inconsistencies were detected
	 */
	INCONSISTENT;
}
