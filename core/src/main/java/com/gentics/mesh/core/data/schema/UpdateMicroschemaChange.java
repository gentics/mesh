package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.rest.schema.Microschema;

/**
 * Change entry that contains information on how to change microschema specific attributes.
 */
public interface UpdateMicroschemaChange extends FieldSchemaContainerUpdateChange<Microschema> {

	/**
	 * Set the field order.
	 * 
	 * @param list
	 * 
	 */
	void setOrder(String... fieldNames);

	/**
	 * Return the field order.
	 * 
	 * @return
	 */
	List<String> getOrder();
	
}
