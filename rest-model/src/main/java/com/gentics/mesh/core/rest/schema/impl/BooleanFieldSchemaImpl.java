package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;

public class BooleanFieldSchemaImpl extends AbstractFieldSchema implements BooleanFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.BOOLEAN.toString();
	}
}
