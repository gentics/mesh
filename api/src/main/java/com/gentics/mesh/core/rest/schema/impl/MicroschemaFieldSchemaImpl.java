package com.gentics.mesh.core.rest.schema.impl;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.MicroschemaListableField;
import com.gentics.mesh.core.rest.schema.MicroschemaFieldSchema;

public class MicroschemaFieldSchemaImpl extends AbstractFieldSchema implements MicroschemaFieldSchema {

	@JsonProperty("allow")
	private String[] allowedMicroSchemas;

	private List<? extends MicroschemaListableField> defaultValues = new ArrayList<>();

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
	public List<? extends MicroschemaListableField> getFields() {
		return defaultValues;
	}

}
