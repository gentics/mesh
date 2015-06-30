package com.gentics.mesh.core.rest.schema.impl;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.response.FieldTypes;
import com.gentics.mesh.core.rest.schema.MicroschemaFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaListableFieldSchema;

public class MicroschemaFieldSchemaImpl extends AbstractFieldSchema implements MicroschemaFieldSchema {

	@JsonProperty("allow")
	private String[] allowedMicroSchemas;

	private List<MicroschemaListableFieldSchema> defaultValues = new ArrayList<>();

	@Override
	public String[] getAllowedMicroSchemas() {
		return allowedMicroSchemas;
	}

	@Override
	public void setAllowedMicroSchemas(String[] allowedMicroSchemas) {
		this.allowedMicroSchemas = allowedMicroSchemas;
	}

	@Override
	public String getType() {
		return FieldTypes.MICROSCHEMA.toString();
	}

	@Override
	public List<MicroschemaListableFieldSchema> getFields() {
		return defaultValues;
	}

}
