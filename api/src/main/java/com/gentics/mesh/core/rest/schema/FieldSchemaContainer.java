package com.gentics.mesh.core.rest.schema;

import java.util.List;
import java.util.Optional;

/**
 * A field schema container is a named container that contains field schemas. Typical containers are {@link Schema} or {@link Microschema}.
 */
public interface FieldSchemaContainer {

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
	List<? extends FieldSchema> getFields();

	/**
	 * Add the given field schema to the list of field schemas.
	 * 
	 * @param fieldSchema
	 */
	void addField(FieldSchema fieldSchema);

	/**
	 * Set the list of schema fields.
	 * 
	 * @param fields
	 */
	void setFields(List<FieldSchema> fields);

}
