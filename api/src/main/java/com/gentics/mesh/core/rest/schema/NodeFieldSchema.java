package com.gentics.mesh.core.rest.schema;

public interface NodeFieldSchema extends MicroschemaListableFieldSchema {

	/**
	 * Return the allowed schemas for the node field.
	 * 
	 * @return
	 */
	String[] getAllowedSchemas();

	/**
	 * Set the allowed schema whitelist for the node field.
	 * 
	 * @param allowedSchemas
	 */
	void setAllowedSchemas(String... allowedSchemas);

}
