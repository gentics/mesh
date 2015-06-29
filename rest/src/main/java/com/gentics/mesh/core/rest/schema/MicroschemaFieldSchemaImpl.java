package com.gentics.mesh.core.rest.schema;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

import com.gentics.mesh.core.rest.node.field.Field;

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

}
