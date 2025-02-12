package com.gentics.mesh.core.rest.schema;

import java.util.Optional;

/**
 * Schema field definition for list fields.
 */
public interface ListFieldSchema extends FieldSchema {

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
	// TODO convert the list type to an enum
	ListFieldSchema setListType(String listType);

	@Override
	default Optional<ListFieldSchema> maybeGetListField() {
		return Optional.of(this);
	}
}
