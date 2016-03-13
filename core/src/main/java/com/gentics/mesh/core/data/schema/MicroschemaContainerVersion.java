package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

public interface MicroschemaContainerVersion
		extends GraphFieldSchemaContainerVersion<Microschema, MicroschemaReference, MicroschemaContainerVersion, MicroschemaContainer> {

	/**
	 * Return a list of micronodes to which the microschema has been assigned.
	 * 
	 * @return
	 */
	List<? extends Micronode> getMicronodes();
}
