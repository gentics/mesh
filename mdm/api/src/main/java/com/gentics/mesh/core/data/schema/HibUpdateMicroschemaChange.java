package com.gentics.mesh.core.data.schema;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Change entry that contains information on how to change microschema specific attributes.
 */
public interface HibUpdateMicroschemaChange extends HibFieldSchemaContainerUpdateChange<MicroschemaModel> {

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


	@Override
	default Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		return Collections.emptyMap();
	}

	@Override
	default SchemaChangeOperation getOperation() {
		return OPERATION;
	}
}
