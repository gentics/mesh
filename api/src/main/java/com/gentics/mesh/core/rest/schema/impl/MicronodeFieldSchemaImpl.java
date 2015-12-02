package com.gentics.mesh.core.rest.schema.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;

public class MicronodeFieldSchemaImpl extends AbstractFieldSchema implements MicronodeFieldSchema {

	@JsonProperty("allow")
	private String[] allowedMicroSchemas;

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
		return FieldTypes.MICRONODE.toString();
	}
}
