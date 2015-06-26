package com.gentics.mesh.core.rest.node.response.field;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

public class MicroschemaFieldProperty extends AbstractFieldProperty {

	@JsonProperty("allow")
	private String[] allowedMicroSchemas;

	private Map<String, FieldProperty> defaultValues = new HashMap<>();

	public String[] getAllowedMicroSchemas() {
		return allowedMicroSchemas;
	}

	public void setAllowedMicroSchemas(String[] allowedMicroSchemas) {
		this.allowedMicroSchemas = allowedMicroSchemas;
	}

	public Map<String, FieldProperty> getDefaultValues() {
		return defaultValues;
	}

	@Override
	public String getType() {
		return PropertyFieldTypes.MICROSCHEMA.toString();
	}

}
