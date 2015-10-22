package com.gentics.mesh.core.rest.schema;

import java.util.List;

/**
 * A microschema field schema is a blueprint for a microschema field within a node. It is possible to restrict the allowed microschemas for the field.
 */
public interface MicroschemaFieldSchema extends FieldSchema {

	/**
	 * Return a list of microschemas which are allowed for this field.
	 * 
	 * @return Allowed schemas
	 */
	String[] getAllowedMicroSchemas();

	/**
	 * Set the list of microschemas which are allowed for this field.
	 * 
	 * @param allowedMicroSchemas
	 *            Allowed schemas
	 */
	void setAllowedMicroSchemas(String[] allowedMicroSchemas);

	/**
	 * Return a list of microschemas for this field schema.
	 * 
	 * @return List of microschemas
	 */
	List<MicroschemaListableFieldSchema> getFields();

}
