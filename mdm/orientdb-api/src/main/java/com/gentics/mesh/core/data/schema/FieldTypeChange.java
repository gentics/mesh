package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Change entry which contains information for a field type change.
 */
public interface FieldTypeChange extends SchemaFieldChange {

	SchemaChangeOperation OPERATION = SchemaChangeOperation.CHANGEFIELDTYPE;

	/**
	 * Return the new field type value.
	 * 
	 * @return
	 */
	String getType();

	/**
	 * Set the new field type value.
	 * 
	 * @param type
	 */
	void setType(String type);

	/**
	 * Return the new list type value.
	 * 
	 * @return
	 */
	String getListType();

	/**
	 * Set the new list type value.
	 * 
	 * @param listType
	 */
	void setListType(String listType);

}
