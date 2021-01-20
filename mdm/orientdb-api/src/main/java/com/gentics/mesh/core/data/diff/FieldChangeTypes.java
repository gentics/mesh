package com.gentics.mesh.core.data.diff;

/**
 * List of change types for schema diff results.
 */
public enum FieldChangeTypes {

	/**
	 * The field has been added
	 */
	ADDED,

	/**
	 * The field has been removed
	 */
	REMOVED,

	/**
	 * The field properties have been updated
	 */
	UPDATED;
}
