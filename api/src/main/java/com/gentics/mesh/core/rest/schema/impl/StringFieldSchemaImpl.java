package com.gentics.mesh.core.rest.schema.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;

public class StringFieldSchemaImpl extends AbstractFieldSchema implements StringFieldSchema {
	@JsonProperty("allow")
	private String[] allowedValues;

	@Override
	public String getType() {
		return FieldTypes.STRING.toString();
	}

	@Override
	public String[] getAllowedValues() {
		return allowedValues;
	}

	@Override
	public void setAllowedValues(String[] allowedValues) {
		this.allowedValues = allowedValues;
	}
}
