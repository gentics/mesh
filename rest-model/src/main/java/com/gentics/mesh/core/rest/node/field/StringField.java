package com.gentics.mesh.core.rest.node.field;

public interface StringField extends ListableField, MicroschemaListableField {

	/**
	 * Return the string value of the field.
	 * 
	 * @return String field value
	 */
	String getString();

	/**
	 * Set the string value of the field.
	 * 
	 * @param string
	 *            String field value
	 * @return Fluent API
	 */
	StringField setString(String string);

	@Override
	default Object getValue() {
		return getString();
	}
}
