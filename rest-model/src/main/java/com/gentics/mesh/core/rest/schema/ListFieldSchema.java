package com.gentics.mesh.core.rest.schema;

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

	// /**
	// * Return the maximum of items that the list is allowed to hold.
	// *
	// * @return
	// */
	// Integer getMax();
	//
	// /**
	// * Set the maximum of items that the list can hold.
	// *
	// * @param max The max item limit
	// * @return Fluent API
	// */
	// ListFieldSchema setMax(Integer max);
	//
	// /**
	// * Return the minimum of items that the list is allowed to hold.
	// *
	// * @return
	// */
	// Integer getMin();
	//
	// /**
	// * Set the minimum of items that the list is allowd to hold
	// *
	// * @param min
	// * @return Fluent API
	// */
	// ListFieldSchema setMin(Integer min);
}
