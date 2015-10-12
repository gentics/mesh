package com.gentics.mesh.core.rest.schema;

public interface ListFieldSchema extends MicroschemaListableFieldSchema {

	String[] getAllowedSchemas();

	ListFieldSchema setAllowedSchemas(String[] allowedSchemas);

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
	 * @return
	 */
	ListFieldSchema setListType(String listType);

	/**
	 * Return the maximum of items that the list is allowed to hold.
	 * 
	 * @return
	 */
	Integer getMax();

	/**
	 * Set the maximum of items that the list can hold.
	 * 
	 * @param max
	 * @return
	 */
	ListFieldSchema setMax(Integer max);

	/**
	 * Return the minimum of items that the list is allowed to hold.
	 * 
	 * @return
	 */
	Integer getMin();

	ListFieldSchema setMin(Integer min);
}
