package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.schema.StringFieldSchema;

public class StringFieldSchemaImpl extends AbstractFieldSchema implements StringFieldSchema {

	String defaultValue;

	@Override
	public String getText() {
		return defaultValue;
	}

	@Override
	public void setText(String text) {
		this.defaultValue = text;
	}
}
