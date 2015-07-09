package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;

public class StringFieldSchemaImpl extends AbstractFieldSchema implements StringFieldSchema {

	String defaultValue;

	@Override
	public String getString() {
		return defaultValue;
	}

	@Override
	public void setString(String text) {
		this.defaultValue = text;
	}
	
	@Override
	public String getType() {
		return FieldTypes.STRING.toString();
	}
}
