package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.core.data.schema.UpdateMicroschemaChange;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * @see UpdateMicroschemaChange
 */
public class UpdateMicroschemaChangeImpl extends AbstractFieldSchemaContainerUpdateChange<Microschema> implements UpdateMicroschemaChange {

	@Override
	public SchemaChangeOperation getOperation() {
		return OPERATION;
	}

}
