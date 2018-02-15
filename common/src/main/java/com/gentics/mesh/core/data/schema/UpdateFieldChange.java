package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Change entry which contains information for a field update. This includes field specific settings.
 */
public interface UpdateFieldChange extends SchemaFieldChange {

	SchemaChangeOperation OPERATION = SchemaChangeOperation.UPDATEFIELD;

	/**
	 * Return the field label.
	 * 
	 * @return
	 */
	String getLabel();

	/**
	 * Set the field label.
	 * 
	 * @param label
	 */
	void setLabel(String label);

}
