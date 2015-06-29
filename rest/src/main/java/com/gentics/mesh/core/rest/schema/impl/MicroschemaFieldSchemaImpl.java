package com.gentics.mesh.core.rest.schema.impl;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.response.FieldTypes;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.MicroschemaFieldSchema;

public class MicroschemaFieldSchemaImpl extends AbstractFieldSchema implements MicroschemaFieldSchema {

	@JsonProperty("allow")
	private String[] allowedMicroSchemas;
	private Map<String, Field> defaultValues = new HashMap<>();

	@Override
	public String[] getAllowedMicroSchemas() {
		return allowedMicroSchemas;
	}

	@Override
	public void setAllowedMicroSchemas(String[] allowedMicroSchemas) {
		this.allowedMicroSchemas = allowedMicroSchemas;
	}

	@Override
	public Map<String, Field> getDefaultValues() {
		return defaultValues;
	}

	@Override
	public String getType() {
		return FieldTypes.MICROSCHEMA.toString();
	}

}
