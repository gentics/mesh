package com.gentics.mesh.core.rest.schema;

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
