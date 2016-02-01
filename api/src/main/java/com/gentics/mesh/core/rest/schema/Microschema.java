package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.json.MeshJsonException;

/**
 * Interface for a Microschema. A microschema can be used to create objects that are embedded into other objects.
 */
public interface Microschema extends FieldSchemaContainer {

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
	 * Validate the microschema for correctness.
	 * 
	 * @throws MeshJsonException
	 */
	void validate() throws MeshJsonException;

}
