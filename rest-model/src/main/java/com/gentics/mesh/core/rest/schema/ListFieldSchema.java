package com.gentics.mesh.core.rest.schema;

public interface ListFieldSchema extends FieldSchema, SchemaRestriction {

	/**
	 * Return the list of allowed schemas.
	 * 
	 * @return
	 */
	String[] getAllowedSchemas();

	/**
	 * Define a list of allowed schemas.
	 * 
	 * @param allowedSchemas
	 * @return
	 */
	ListFieldSchema setAllowedSchemas(String... allowedSchemas);

	/**
	 * Return the list type (string, number, date...)
	 * 
	 * @return
	 */
	String getListType();

	/**
	 * Set the list type.
	 * 
	 * @param listType
	 *            List type
	 * @return Fluent API
	 */
	//TODO convert the list type to an enum
	ListFieldSchema setListType(String listType);

	/**
	 * Tests if a schema is allowed according to the schema restrictions.
	 * @param schemaName The name of the schema to be tested
	 */
	@Override
	default boolean isAllowedSchema(String schemaName) {
		return SchemaRestriction.super.isAllowedSchema(schemaName);
	}
}
