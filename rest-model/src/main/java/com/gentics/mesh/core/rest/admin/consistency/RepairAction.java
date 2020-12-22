package com.gentics.mesh.core.rest.admin.consistency;

/**
 * Consistency check repair actions.
 */
public enum RepairAction {

	/**
	 * No action possible.
	 */
	NONE,

	/**
	 * The element can be recovered.
	 */
	RECOVER,

	/**
	 * The element which causes the inconsistency can be deleted.
	 */
	DELETE
}
