package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Change entry that contains information on how to change microschema specific attributes.
 */
public interface UpdateMicroschemaChange extends FieldSchemaContainerUpdateChange<Microschema> {

	SchemaChangeOperation OPERATION = SchemaChangeOperation.UPDATEMICROSCHEMA;

	/**
	 * Set the field order.
	 * 
	 * @param fieldNames
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
