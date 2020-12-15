package com.gentics.mesh.core.rest.admin.consistency;

/**
 * Consistency check results.
 */
public enum InconsistencySeverity {
	/**
	 * Major inconsistency which impact multiple elements or the whole operation
	 */
	CRITICAL,

	/**
	 * High inconsistency which impacts one element.
	 */
	HIGH,

	/**
	 * Medium inconsistency which impacts one element.
	 */
	MEDIUM,

	/**
	 * Low severity inconsistency
	 */
	LOW;
}
