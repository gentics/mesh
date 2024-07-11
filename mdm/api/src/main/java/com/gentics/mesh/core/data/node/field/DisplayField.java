package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.Field;

/**
 * Represents a field that can be used as a display field.
 */
public interface DisplayField extends Field {

	/**
	 * Gets the string representation of the field.
	 * 
	 * @return Display field value
	 */
	String getDisplayName();
}
