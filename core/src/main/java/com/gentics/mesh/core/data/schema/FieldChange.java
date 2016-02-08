package com.gentics.mesh.core.data.schema;

/**
 * Change entry which contains information on how to handle a field (eg. change type, settings)
 */
public interface FieldChange extends SchemaFieldChange {

	public static final String FIELD_PROPERTY_PREFIX_KEY = "fieldProperty_";

	/**
	 * Set a field specific property.
	 * 
	 * @param key
	 * @param value
	 */
	void setFieldProperty(String key, String value);

	/**
	 * Return a field specific property.
	 * 
	 * @param key
	 * @return
	 */
	String getFieldProperty(String key);
}
