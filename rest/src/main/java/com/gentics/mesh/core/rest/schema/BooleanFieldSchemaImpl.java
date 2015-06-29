package com.gentics.mesh.core.rest.schema;


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

}
