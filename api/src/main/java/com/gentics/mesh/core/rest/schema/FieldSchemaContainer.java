package com.gentics.mesh.core.rest.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;

/**
 * A field schema container is a named container that contains field schemas. Typical containers are {@link Schema} or {@link Microschema}.
 */
public interface FieldSchemaContainer extends RestModel {

	/**
	 * Return the name of the container.
	 * 
	 * @return Name of the container
	 */
	String getName();

	/**
	 * Set the container name.
	 * 
	 * @param name
	 *            Name of the container
	 * @return 
	 */
	void setName(String name);

	/**
	 * Return the container description.
	 * 
	 * @return Schema description
	 */
	String getDescription();

	/**
	 * Set the description of the container.
	 * 
	 * @param description
	 *            Container description
	 */
	void setDescription(String description);

	/**
	 * Return the field with the given name.
	 * 
	 * @param fieldName
	 * @return
	 */
	default FieldSchema getField(String fieldName) {
		return (FieldSchema) getFields().stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
	}

	/**
	 * Return the field schema with the given name.
	 * 
	 * @param fieldName
	 * @param classOfT
	 * @return
	 */
	default <T> T getField(String fieldName, Class<T> classOfT) {
		return (T) getFields().stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
	}

	/**
	 * Removes the field with the given name.
	 * 
	 * @param name
	 */
	default void removeField(String name) {
		if (name == null) {
			return;
		}
		getFields().removeIf(field -> name.equals(field.getName()));
	}

	/**
	 * Return the list of field schemas.
	 * 
	 * @return List of field schemas
	 */
	List<FieldSchema> getFields();

	/**
	 * Return the map of field schemas.
	 * 
	 * @return
	 */
	@JsonIgnore
	default Map<String, FieldSchema> getFieldsAsMap() {
		Map<String, FieldSchema> map = new HashMap<>();
		for (FieldSchema field : getFields()) {
			map.put(field.getName(), field);
		}
		return map;
	}

	/**
	 * Add the given field schema to the list of field schemas.
	 * 
	 * @param fieldSchema
	 */
	default void addField(FieldSchema fieldSchema) {
		Objects.requireNonNull(fieldSchema, "The field schema must not be null");
		Objects.requireNonNull(fieldSchema.getName(), "The field schema must have a valid name");
		getFields().add(fieldSchema);
	}

	/**
	 * Add the given field schema to the list of field schemas after the field with the given name. The field will be added to the end of the list if the insert
	 * position could not be determined via the afterFieldName parameter.
	 * 
	 * @param fieldSchema
	 *            Field schema to be added to the container
	 * @param afterFieldName
	 *            Field name that identifies the position after which the field will be inserted
	 */
	default void addField(FieldSchema fieldSchema, String afterFieldName) {
		List<FieldSchema> fields = getFields();
		int index = fields.size();
		if (afterFieldName != null) {
			for (int i = 0; i < fields.size(); i++) {
				if (afterFieldName.equals(fields.get(i).getName())) {
					index = i;
					break;
				}
			}
		}
		if (index < fields.size()) {
			index = index + 1;
		}
		fields.add(index, fieldSchema);
	}

	/**
	 * Set the list of schema fields.
	 * 
	 * @param fields
	 */
	void setFields(List<FieldSchema> fields);

	/**
	 * Return the container version.
	 * 
	 * @return
	 */
	int getVersion();

	/**
	 * Set the container version.
	 * 
	 * @param version
	 */
	void setVersion(int version);

	/**
	 * Validate the schema for correctness.
	 */
	default void validate() {
		if (StringUtils.isEmpty(getName())) {
			throw error(BAD_REQUEST, "schema_error_no_name");
		}

		Set<String> fieldLabels = new HashSet<>();
		Set<String> fieldNames = new HashSet<>();

		for (FieldSchema field : getFields()) {
			if (field.getName() != null) {
				if (!fieldNames.add(field.getName())) {
					throw error(BAD_REQUEST, "schema_error_duplicate_field_name", field.getName());
				}
			}

			if (field.getLabel() != null) {
				if (!fieldLabels.add(field.getLabel())) {
					throw error(BAD_REQUEST, "schema_error_duplicate_field_label", field.getName(), field.getLabel());
				}
			}
			field.validate();
		}
	}


	/**
	 * Assert that the field map does not contain any fields which are not specified by the schema.
	 * 
	 * @param fieldMap
	 */
	default void assertForUnhandledFields(FieldMap fieldMap) {
		Set<String> allFieldsOfRequest = new HashSet<>(fieldMap.keySet());
		for (FieldSchema fieldSchema : getFields()) {
			allFieldsOfRequest.remove(fieldSchema.getName());
		}
		if (allFieldsOfRequest.size() > 0) {
			throw error(BAD_REQUEST, "node_unhandled_fields", getName(), Arrays.toString(allFieldsOfRequest.toArray()));
		}
	}
}
