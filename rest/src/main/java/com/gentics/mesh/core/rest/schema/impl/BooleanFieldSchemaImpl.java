package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.response.FieldTypes;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;

public class BooleanFieldSchemaImpl extends AbstractFieldSchema implements BooleanFieldSchema {

	private Boolean defaultValue;

	@Override
	public void setValue(Boolean value) {
		defaultValue = value;
	}

	@Override
	public Boolean getValue() {
		return defaultValue;
	}

	@Override
	public String getType() {
		return FieldTypes.BOOLEAN.toString();
	}

}
