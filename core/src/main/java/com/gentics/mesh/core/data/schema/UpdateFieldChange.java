package com.gentics.mesh.core.data.schema;

public interface UpdateFieldChange extends SchemaFieldChange {

	/**
	 * Return the field label.
	 * 
	 * @return
	 */
	String getLabel();

	/**
	 * Set the field label.
	 * 
	 * @param string
	 */
	void setLabel(String label);

}
