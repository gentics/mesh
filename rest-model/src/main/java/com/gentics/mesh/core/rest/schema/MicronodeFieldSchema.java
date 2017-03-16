package com.gentics.mesh.core.rest.schema;

/**
 * A micronode field schema is a blueprint for a micronode field within a node. It is possible to restrict the allowed microschemas for the field.
 */
public interface MicronodeFieldSchema extends FieldSchema {

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
	 * @return Fluent API
	 */
	MicronodeFieldSchema setAllowedMicroSchemas(String... allowedMicroSchemas);
}
