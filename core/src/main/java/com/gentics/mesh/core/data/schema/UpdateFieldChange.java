package com.gentics.mesh.core.data.schema;

/**
 * Change entry which contains information for a field update. This includes field specific settings.
 */
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
	 * @param label
	 */
	void setLabel(String label);

}
