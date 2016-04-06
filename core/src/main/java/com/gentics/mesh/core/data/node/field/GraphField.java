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
	 * Remove this field from the container
	 * 
	 * @param container
	 *            container
	 */
	void removeField(GraphFieldContainer container);

	/**
	 * Clone this field into the given container. If the field uses extra vertices for storing the data, they must be reused, not copied
	 *
	 * @param container
	 * @return cloned field
	 */
	GraphField cloneTo(GraphFieldContainer container);

	/**
	 * Validate consistency of this field. If the field contains micronodes, this will check, whether all mandatory fields have been filled
	 */
	void validate();

	/**
	 * Compares this field value to the specified graph field. The result is {@code
	 * true} if and only if the argument is not {@code null} and is a {@code
	 * GraphField} object that represents the same value and type as this field.
	 *
	 * @param field
	 *            The field to compare this {@code GrpahField} against
	 *
	 * @return {@code true} if the given field value represents a {@code GraphField} which is of the same type as this field and if its value is equivalent to
	 *         this field value, {@code false} otherwise
	 */
	boolean equals(GraphField field);
}
