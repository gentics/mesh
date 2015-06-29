package com.gentics.mesh.core.rest.schema;


public class DateFieldSchemaImpl extends AbstractFieldSchema implements DateFieldSchema {

	private String defaultValue;

	@Override
	public void setDate(String date) {
		this.defaultValue = date;

	}

	@Override
	public String getDate() {
		return this.defaultValue;
	}

}
