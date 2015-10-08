package com.gentics.mesh.core.rest.node.field;

public interface StringField extends ListableField, MicroschemaListableField {

	/**
	 * Return the string value of the field.
	 * 
	 * @return
	 */
	String getString();

	/**
	 * Set the string value of the field.
	 * 
	 * @param string
	 * @return fluent API
	 */
	StringField setString(String string);

}
