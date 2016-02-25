package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * A microschema container is a graph element which stores the JSON microschema data.
 */
public interface MicroschemaContainer extends GraphFieldSchemaContainer<Microschema, MicroschemaContainer, MicroschemaReference> {

	public static final String TYPE = "microschema";

	/**
	 * Return a list of micronodes to which the microschema has been assigned.
	 * 
	 * @return
	 */
	List<? extends Micronode> getMicronodes();

}
