package com.gentics.mesh.core.data.schema.handler;

/**
 * Enum that is used within the {@link SchemaComparator} in order to classify schema changes.
 */
public enum Difference {
	/**
	 * Element did not change
	 */
	SAME,
	/**
	 * Element was added
	 * 
	 */
	ADDED,
	/**
	 * Element was removed
	 */
	REMOVED,
	/**
	 * Element was updated
	 */
	CHANGED
}
