package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;

/**
 * A microschema container is a graph element which stores the JSON microschema data.
 */
public interface MicroschemaContainer extends MeshCoreVertex<MicroschemaResponse, MicroschemaContainer>, ReferenceableElement<MicroschemaReference> {

	public static final String TYPE = "microschema";

	/**
	 * Get the microschema stored in this container
	 * 
	 * @return microschema instance
	 */
	Microschema getMicroschema();

	/**
	 * Set the microschema for this container
	 * 
	 * @param microschema
	 *            microschema instance
	 */
	void setMicroschema(Microschema microschema);

//	/**
//	 * Return the microschema description text.
//	 * 
//	 * @return
//	 */
//	String getDescription();
//
//	/**
//	 * Set the microschema description.
//	 * 
//	 * @param text
//	 */
//	void setDescription(String text);
}
