package com.gentics.mesh.core.rest.schema;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

/**
 * A field schema is a field within a schema. In contradiction to node fields a field schema is the blueprint of a field and will not store any data. Instead it
 * only defines a field within a schema.
 */
public interface FieldSchema {

	/**
	 * Return the type of the field schema.
	 * 
	 * @return Field schema type
	 */
	String getType();

	/**
	 * Return the label of the field schema.
	 * 
	 * @return Label
	 */
	String getLabel();

	/**
	 * Set the label of the field schema.
	 * 
	 * @param label
	 *            Field schema label
	 * @return Fluent API
	 */
	FieldSchema setLabel(String label);

	//TODO is this not the fieldkey? is the key the name??
	/**
	 * Return the name of the field schema.
	 * 
	 * @return Name
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
	 * 
	 * @return
	 */
	boolean isRequired();

	/**
	 * Set the required flag.
	 * 
	 * @param isRequired
	 * @return Fluent API
	 */
	FieldSchema setRequired(boolean isRequired);

	/**
	 * Compare the field schema with the given field schema.
	 * 
	 * @param fieldSchema
	 * @return Detected change
	 * @throws IOException
	 */
	SchemaChangeModel compareTo(FieldSchema fieldSchema) throws IOException;

	/**
	 * Apply the given field properties to the field schema.
	 * 
	 * @param fieldProperties
	 */
	void apply(Map<String, Object> fieldProperties);

	/**
	 * Validate the field properties.
	 */
	void validate();

	Map<String, Object> getAllChangeProperties();

}
