package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * A microschema container is a graph element which stores the JSON microschema data.
 */
public interface MicroschemaContainer
		extends GraphFieldSchemaContainer<Microschema, MicroschemaReference, MicroschemaContainer, MicroschemaContainerVersion> {

	public static final String TYPE = "microschema";

}
