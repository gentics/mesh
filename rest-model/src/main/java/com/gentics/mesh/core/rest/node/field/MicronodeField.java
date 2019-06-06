package com.gentics.mesh.core.rest.node.field;

import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * Interface for micronode fields
 */
public interface MicronodeField extends ListableField, FieldContainer {

	/**
	 * Return the uuid of the micronode.
	 * 
	 * @return Uuid of the micronode
	 */
	String getUuid();

	/**
	 * Get the microschema reference used for the micronode.
	 * 
	 * @return microschema reference
	 */
	MicroschemaReference getMicroschema();

	@Override
	default Object getValue() {
		return getUuid();
	}
}
