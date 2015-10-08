package com.gentics.mesh.core.data.node.field;

public interface GraphField {

	public static final String FIELD_KEY_PROPERTY_KEY = "fieldkey";

	/**
	 * Set the graph field key.
	 * 
	 * @param key
	 */
	void setFieldKey(String key);

	/**
	 * Return the graph field key.
	 * 
	 * @return
	 */
	String getFieldKey();
}
