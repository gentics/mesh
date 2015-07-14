package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;

public class StringFieldSchemaImpl extends AbstractFieldSchema implements StringFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.STRING.toString();
	}
}
