package com.gentics.mesh.core.rest.node.field;

public interface BooleanField extends ListableField, MicroschemaListableField {

	/**
	 * Set the boolean value of the boolean field.
	 * 
	 * @param value
	 * @return fluent API
	 */
	BooleanField setValue(Boolean value);

	/**
	 * Return the boolean value of the boolean field.
	 * 
	 * @return
	 */
	Boolean getValue();

}
