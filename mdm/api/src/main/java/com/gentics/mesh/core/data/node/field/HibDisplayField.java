package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibField;

/**
 * Represents a field that can be used as a display field.
 */
public interface HibDisplayField extends HibField {

	/**
	 * Gets the string representation of the field.
	 * 
	 * @return Display field value
	 */
	String getDisplayName();
}
