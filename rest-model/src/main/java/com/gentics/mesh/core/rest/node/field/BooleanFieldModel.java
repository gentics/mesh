package com.gentics.mesh.core.rest.node.field;

/**
 * Node field definition for a boolean field. The extended marker interfaces are used to allow nesting in fields and in micronodes.
 */
public interface BooleanFieldModel extends ListableFieldModel, MicroschemaListableFieldModel {

	/**
	 * Set the boolean value of the boolean field.
	 * 
	 * @param value
	 * @return fluent API
	 */
	BooleanFieldModel setValue(Boolean value);

	/**
	 * Return the boolean value of the boolean field.
	 * 
	 * @return
	 */
	Boolean getValue();

}
