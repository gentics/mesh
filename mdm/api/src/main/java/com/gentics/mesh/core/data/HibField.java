package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

public interface HibField extends HibFieldKeyElement {

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
	public static void failOnMissingRequiredField(HibField field, boolean isFieldNull, FieldSchema fieldSchema, String key,
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
	public static void failOnDeletionOfRequiredField(HibField graphField, boolean isFieldSetToNull, FieldSchema fieldSchema, String key,
		FieldSchemaContainer schema) {
		// Field is required and already set and value is null -> deletion is not allowed for required fields
		if (fieldSchema.isRequired() && graphField != null && isFieldSetToNull) {
			throw error(BAD_REQUEST, "node_error_required_field_not_deletable", key, schema.getName());
		}
	}

	/**
	 * Clone this field into the given container. If the field uses extra vertices for storing the data, they must be reused, not copied
	 *
	 * @param container
	 * @return cloned field
	 */
	HibField cloneTo(HibFieldContainer container);

	/**
	 * Validate consistency of this field. If the field contains micronodes, this will check, whether all mandatory fields have been filled
	 */
	void validate();

	/**
	 * Compares this field value to the specified object. The result is {@code
	 * true} if and only if the argument is not {@code null} and is a {@code
	 * Field} or {@code HibField} object that represents the same value and type as this field.
	 *
	 * @param obj
	 *            The rest field to compare this {@code HibField} against
	 *
	 * @return {@code true} if the given field value represents a {@code Field} or {@code HibField} which is of the same type as this field and if its value
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
