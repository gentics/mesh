package com.gentics.mesh.core.rest.node.field;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * Interface for micronode fields
 */
public interface MicronodeField extends ListableField {

	/**
	 * Return the uuid of the micronode.
	 * 
	 * @return Uuid of the micronode
	 */
	String getUuid();

	/**
	 * Get the fields of the micronode.
	 * 
	 * @return field of the micronode
	 */
	FieldMap getFields();

	/**
	 * Get the microschema reference used for the micronode.
	 * 
	 * @return microschema reference
	 */
	MicroschemaReference getMicroschema();
}
