package com.gentics.mesh.parameter.value;

import java.util.Set;

/**
 * Utility interface for string sets.
 */
public interface FieldsSet extends Set<String> {

	/**
	 * Check whether the field should be included.
	 * 
	 * @param fieldName
	 * @return True if the field is part of the set or if the set is empty and no whitelist has been defined
	 */
	default boolean has(String fieldName) {
		return isEmpty() || contains(fieldName);
	}

}
