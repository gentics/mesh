package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.GraphFieldContainer;

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

	/**
	 * Remove this field from its container
	 */
	void removeField();

	/**
	 * Clone this field into the given container.
	 * If the field uses extra vertices for storing the data, they must be reused, not copied
	 *
	 * @param container
	 * @return cloned field
	 */
	GraphField cloneTo(GraphFieldContainer container);
}
