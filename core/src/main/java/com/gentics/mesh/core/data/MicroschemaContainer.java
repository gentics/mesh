package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;

/**
 * A microschema container is a graph element which stores the JSON microschema data.
 */
public interface MicroschemaContainer extends GraphFieldSchemaContainer<MicroschemaResponse, MicroschemaContainer, MicroschemaReference> {

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

	/**
	 * Return a list of micronodes to which the microschema has been assigned.
	 * 
	 * @return
	 */
	List<? extends Micronode> getMicronodes();

}
