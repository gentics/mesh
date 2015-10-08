package com.gentics.mesh.core.rest.schema;

/**
 * A field schema is a field within a schema. In contradiction to node fields a field schema is the blueprint of a field and will not store any data. Instead it
 * only defines a field within a schema.
 */
public interface FieldSchema {

	/**
	 * Return the type of the field schema.
	 * 
	 * @return
	 */
	String getType();

	/**
	 * Return the label of the field schema.
	 * 
	 * @return
	 */
	String getLabel();

	/**
	 * Set the label of the field schema.
	 * 
	 * @param label
	 * @return Fluent API
	 */
	FieldSchema setLabel(String label);

	//TODO is this not the fieldkey? is the key the name??
	/**
	 * Return the name of the field schema.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Set the name of the field schema.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	FieldSchema setName(String name);

	/**
	 * Return the required flag of the field schema.
	 * @return
	 */
	boolean isRequired();

	/**
	 * 
	 * @param flag
	 * @return Fluent API
	 */
	FieldSchema setRequired(boolean flag);
}
