package com.gentics.mesh.core.data.node.field;

/**
 * Represents a field that can be used as a display field.
 */
public interface DisplayField extends GraphField {

	/**
	 * Gets the string representation of the field.
	 * 
	 * @return Display field value
	 */
	String getDisplayName();
}