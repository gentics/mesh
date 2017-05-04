package com.gentics.mesh.core.rest.schema;

public interface NodeFieldSchema extends FieldSchema {

	/**
	 * Return the allowed schemas for the node field.
	 * 
	 * @return
	 */
	String[] getAllowedSchemas();

	/**
	 * Set the allowed schema white list for the node field.
	 * 
	 * @param allowedSchemas
	 * @return Fluent API 
	 */
	NodeFieldSchema setAllowedSchemas(String... allowedSchemas);

}
