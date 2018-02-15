package com.gentics.mesh.core.rest.schema;

public interface NodeFieldSchema extends FieldSchema, SchemaRestriction {

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

	/**
	 * Tests if a schema is allowed according to the schema restrictions.
	 * @param schemaName The name of the schema to be tested
	 */
	@Override
	default boolean isAllowedSchema(String schemaName) {
		return SchemaRestriction.super.isAllowedSchema(schemaName);
	}
}
