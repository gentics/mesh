package com.gentics.mesh.core.rest.schema;

public interface StringFieldSchema extends FieldSchema {

	/**
	 * Return a list of values which are allowed for this field. Null if no value restriction set
	 * 
	 * @return Allowed values
	 */
	String[] getAllowedValues();

	/**
	 * Set the list of values which are allowed for this field. Set to null to remove value restriction
	 * 
	 * @param allowedValues
	 *            Allowed values or null
	 * @return Fluent API
	 */
	StringFieldSchema setAllowedValues(String... allowedValues);
}
