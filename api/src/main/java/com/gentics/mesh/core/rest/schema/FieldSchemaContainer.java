package com.gentics.mesh.core.rest.schema;

import java.util.List;
import java.util.Optional;

/**
 * A field schema container is a container that contains field schemas. Typical containers are {@link Schema} or {@link Microschema}.
 */
public interface FieldSchemaContainer {

	/**
	 * Return the name of the microschema.
	 * 
	 * @return Name of the microschema
	 */
	String getName();

	/**
	 * Set the microschema name.
	 * 
	 * @param name
	 *            Name of the microschema
	 */
	void setName(String name);

	/**
	 * Return the field schema with the given name.
	 * 
	 * @param fieldName
	 * @return
	 */
	Optional<FieldSchema> getFieldSchema(String fieldName);

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

}
