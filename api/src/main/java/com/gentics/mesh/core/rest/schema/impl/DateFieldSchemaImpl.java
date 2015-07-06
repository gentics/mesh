package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;

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

	@Override
	public String getType() {
		return FieldTypes.DATE.toString();
	}

}
