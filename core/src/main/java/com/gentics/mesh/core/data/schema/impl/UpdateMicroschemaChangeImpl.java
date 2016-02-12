package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.core.data.schema.UpdateMicroschemaChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;

/**
 * @see UpdateMicroschemaChange
 */
public class UpdateMicroschemaChangeImpl extends AbstractFieldSchemaContainerUpdateChange<Microschema> implements UpdateMicroschemaChange {

	@Override
	public <R extends FieldSchemaContainer> R apply(R container) {
		// TODO Auto-generated method stub
		return super.apply(container);
	}

}
