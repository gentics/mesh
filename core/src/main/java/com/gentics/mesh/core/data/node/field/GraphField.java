package com.gentics.mesh.core.data.node.field;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface GraphField {

	String FIELD_KEY_PROPERTY_KEY = "fieldkey";

	/**
	 * Throw an error if all assumptions match:
	 * <ul>
	 * <li>The given field has not yet been created</li>
	 * <li>The field is mandatory</li>
	 * <li>The rest field does not contain any data</li>
	 * </ul>
	 * 
	 * @param ac
	 * @param field
	 * @param restField
	 * @param fieldSchema
	 * @param key
	 * @param schema
	 * @throws HttpStatusCodeErrorException
	 */
	static void failOnMissingMandatoryField(InternalActionContext ac, GraphField field, Field restField, FieldSchema fieldSchema, String key,
			FieldSchemaContainer schema) throws HttpStatusCodeErrorException {
		if (field == null && fieldSchema.isRequired() && restField == null) {
			throw error(BAD_REQUEST, "node_error_missing_mandatory_field_value", key, schema.getName());
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
