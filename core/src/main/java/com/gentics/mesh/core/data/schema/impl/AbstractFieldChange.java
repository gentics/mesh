package com.gentics.mesh.core.data.schema.impl;

/**
 * Common field change class which may be used for changes that target a specific field.
 */
public class AbstractFieldChange extends AbstractSchemaChange {

	private static final String FIELDNAME_KEY = "fieldName";

	/**
	 * Return the field name which should be handled.
	 * 
	 * @return field name
	 */
	public String getFieldName() {
		return getProperty(FIELDNAME_KEY);
	}

	/**
	 * Set the name of the field which should be handled.
	 * 
	 * @param name
	 *            field name
	 */
	public void setFieldName(String name) {
		setProperty(FIELDNAME_KEY, name);
	}

}
