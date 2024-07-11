package com.gentics.mesh.core.rest.node.field;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;

/**
 * REST POJO for string field information. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public interface StringFieldModel extends ListableFieldModel, MicroschemaListableFieldModel {

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
	StringFieldModel setString(String string);

	@Override
	default Object getValue() {
		return getString();
	}

	/**
	 * Convenience method of creating a simple string field.
	 * 
	 * @param value
	 * @return
	 */
	static StringFieldModel of(String value) {
		return new StringFieldImpl().setString(value);
	}
}
