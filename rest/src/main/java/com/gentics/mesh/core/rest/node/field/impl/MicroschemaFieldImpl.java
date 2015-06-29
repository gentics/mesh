package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.node.field.MicroschemaField;
import com.gentics.mesh.model.FieldTypes;

public class MicroschemaFieldImpl implements MicroschemaField {

	@Override
	public String getType() {
		return FieldTypes.MICROSCHEMA.toString();
	}

}
