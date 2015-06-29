package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.common.response.FieldTypes;
import com.gentics.mesh.core.rest.node.field.MicroschemaField;

public class MicroschemaFieldImpl implements MicroschemaField {

	@Override
	public String getType() {
		return FieldTypes.MICROSCHEMA.toString();
	}

}
