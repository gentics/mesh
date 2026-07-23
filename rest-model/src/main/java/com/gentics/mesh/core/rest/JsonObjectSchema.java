package com.gentics.mesh.core.rest;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.json.JsonUtil;

/**
 * JSON Schema for objects.
 */
public class JsonObjectSchema extends JsonSchemaType implements JsonSchema {

	private String[] required = new String[0];
	private Map<String, JsonSchemaType> properties = new HashMap<>();

	public JsonObjectSchema() {
		super("object");
	}

	public JsonObjectSchema(String[] required, Map<String, JsonSchemaType> properties) {
		this();
		this.required = required;
		this.properties = properties;
	}

	@Override
	public JsonObjectSchema setType(String type) {
		super.setType(type);
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof RestModel) ? JsonUtil.equals(this, (RestModel) obj) : false;
	}

	@Override
	public int hashCode() {
		return JsonUtil.toJson(this).hashCode();
	}

	public String[] getRequired() {
		return required;
	}

	public JsonObjectSchema setRequired(String[] required) {
		this.required = required;
		return this;
	}

	public Map<String, JsonSchemaType> getProperties() {
		return properties;
	}

	public JsonObjectSchema setProperties(Map<String, JsonSchemaType> properties) {
		this.properties = properties;
		return this;
	}
}
