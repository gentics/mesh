package com.gentics.mesh.core.data.node.field;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

/**
 * Common interface for all graph fields. Every field has a key and can be removed from a container.
 */
public interface GraphField {

	String FIELD_KEY_PROPERTY_KEY = "fieldkey";

	/**
	 * Fail if the field is required but no valid value has been specified.
	 * 
	 * @param field
	 * @param isFieldNull
	 * @param fieldSchema
	 * @param key
	 * @param schema
	 * @throws GenericRestException
	 */
	public static void failOnMissingRequiredField(GraphField field, boolean isFieldNull, FieldSchema fieldSchema, String key,
		FieldSchemaContainer schema) throws GenericRestException {
		if (field == null && fieldSchema.isRequired() && isFieldNull) {
			throw error(BAD_REQUEST, "node_error_missing_required_field_value", key, schema.getName());
		}
	}

	/**
	 * Fail required field update if the rest field explicitly set to null and thus the graph field should be removed.
	 * 
	 * @param graphField
	 * @param isFieldSetToNull
	 * @param fieldSchema
	 * @param key
	 * @param schema
	 */
	public static void failOnDeletionOfRequiredField(GraphField graphField, boolean isFieldSetToNull, FieldSchema fieldSchema, String key,
		FieldSchemaContainer schema) {
		// Field is required and already set and value is null -> deletion is not allowed for required fields
		if (fieldSchema.isRequired() && graphField != null && isFieldSetToNull) {
			throw error(BAD_REQUEST, "node_error_required_field_not_deletable", key, schema.getName());
		}
	}

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
	 * Remove this field from the container.
	 * 
	 * @param bac
	 * @param container
	 *            container
	 */
	void removeField(BulkActionContext bac, GraphFieldContainer container);

	/**
	 * Remove the field and use a dummy bulk action context.
	 * 
	 * @param container
	 */
	default void removeField(GraphFieldContainer container) {
		removeField(new DummyBulkActionContext(), container);
	}

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
	 * Compares this field value to the specified object. The result is {@code
	 * true} if and only if the argument is not {@code null} and is a {@code
	 * Field} or {@code GraphField} object that represents the same value and type as this field.
	 *
	 * @param obj
	 *            The rest field to compare this {@code GraphField} against
	 *
	 * @return {@code true} if the given field value represents a {@code Field} or {@code GraphField} which is of the same type as this field and if its value
	 *         is equivalent to this field value, {@code false} otherwise
	 */
	boolean equals(Object obj);

	/**
	 * Compare both fields by using the equals implementation and return a field container change list that contains the detected changes.
	 * 
	 * @param field
	 * @return List of detected changes or empty list if no change has been detected
	 */
	default List<FieldContainerChange> compareTo(Object field) {
		if (!equals(field)) {
			return Arrays.asList(new FieldContainerChange(getFieldKey(), FieldChangeTypes.UPDATED));
		}
		return Collections.emptyList();
	}
}
