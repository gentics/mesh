package com.gentics.mesh.core.rest.schema;

import java.util.List;

import com.gentics.mesh.json.MeshJsonException;

/**
 * Interface for a Microschema. A microschema can be used to create objects that are embedded into other objects.
 */
public interface Microschema {

	/**
	 * Return the name of the microschema.
	 * 
	 * @return Name of the microschema
	 */
	String getName();

	/**
	 * Set the microschema name.
	 * 
	 * @param name
	 *            Name of the microschema
	 */
	void setName(String name);

	/**
	 * Return the microschema description.
	 * 
	 * @return microschema description
	 */
	String getDescription();

	/**
	 * Set the description of the microschema.
	 * 
	 * @param description
	 *            microschema description
	 */
	void setDescription(String description);

	/**
	 * Return the list of field schemas.
	 * 
	 * @return List of field schemas
	 */
	List<? extends FieldSchema> getFields();

	/**
	 * Add the given field schema to the list of field schemas.
	 * 
	 * @param fieldSchema
	 */
	void addField(FieldSchema fieldSchema);

	/**
	 * Removes the field with the given name.
	 * 
	 * @param name
	 */
	void removeField(String name);

	/**
	 * Validate the microschema for correctness.
	 * 
	 * @throws MeshJsonException
	 */
	void validate() throws MeshJsonException;
}
