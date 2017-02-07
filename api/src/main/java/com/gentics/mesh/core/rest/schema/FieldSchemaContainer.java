package com.gentics.mesh.core.rest.schema;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.node.FieldMap;

/**
 * A field schema container is a named container that contains field schemas. Typical containers are {@link Schema} or {@link Microschema}.
 */
public interface FieldSchemaContainer extends GenericRestResponse {

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
	 * Return the field schema with the given name.
	 * 
	 * @param fieldName
	 * @return
	 * @deprecated
	 */
	Optional<FieldSchema> getFieldSchema(String fieldName);

	/**
	 * Return the field with the given name.
	 * 
	 * @param fieldName
	 * @return
	 */
	FieldSchema getField(String fieldName);

	/**
	 * Return the field schema with the given name.
	 * 
	 * @param fieldName
	 * @param classOfT
	 * @return
	 */
	<T> T getField(String fieldName, Class<T> classOfT);

	/**
	 * Removes the field with the given name.
	 * 
	 * @param name
	 */
	void removeField(String name);

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
	Map<String, FieldSchema> getFieldsAsMap();

	/**
	 * Add the given field schema to the list of field schemas.
	 * 
	 * @param fieldSchema
	 */
	void addField(FieldSchema fieldSchema);

	/**
	 * Add the given field schema to the list of field schemas after the field with the given name. The field will be added to the end of the list if the insert
	 * position could not be determined via the afterFieldName parameter.
	 * 
	 * @param fieldSchema
	 *            Field schema to be added to the container
	 * @param afterFieldName
	 *            Field name that identifies the position after which the field will be inserted
	 */
	void addField(FieldSchema fieldSchema, String afterFieldName);

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
	void validate();

	/**
	 * Assert that the field map does not contain any fields which are not specified by the schema.
	 * 
	 * @param fieldMap
	 */
	void assertForUnhandledFields(FieldMap fieldMap);
}
