package com.gentics.mesh.core.rest.node.response.field;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

import com.gentics.mesh.model.FieldTypes;

public class MicroschemaField extends AbstractField {

	@JsonProperty("allow")
	private String[] allowedMicroSchemas;

	private Map<String, Field> defaultValues = new HashMap<>();

	public String[] getAllowedMicroSchemas() {
		return allowedMicroSchemas;
	}

	public void setAllowedMicroSchemas(String[] allowedMicroSchemas) {
		this.allowedMicroSchemas = allowedMicroSchemas;
	}

	public Map<String, Field> getDefaultValues() {
		return defaultValues;
	}

	@Override
	public String getType() {
		return FieldTypes.MICROSCHEMA.toString();
	}

}
