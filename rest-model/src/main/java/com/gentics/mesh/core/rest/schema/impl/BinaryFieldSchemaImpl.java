package com.gentics.mesh.core.rest.schema.impl;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.BinaryFieldParserOption;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;

public class BinaryFieldSchemaImpl extends AbstractFieldSchema implements BinaryFieldSchema {

	@JsonProperty("allow")
	@JsonPropertyDescription("Array of allowed mimetypes")
	private String[] allowedMimeTypes;

	@JsonPropertyDescription("The parsing behaviour for this field.")
	private BinaryFieldParserOption parser;

	@Override
	public String[] getAllowedMimeTypes() {
		return allowedMimeTypes;
	}

	@Override
	public BinaryFieldSchema setAllowedMimeTypes(String... allowedMimeTypes) {
		this.allowedMimeTypes = allowedMimeTypes;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.BINARY.toString();
	}

	@Override
	public Map<String, Object> getAllChangeProperties() {
		Map<String, Object> properties = super.getAllChangeProperties();
		properties.put("allow", getAllowedMimeTypes());
		properties.put("parser", getParserOption());
		return properties;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		if (fieldProperties.get("allowedMimeTypes") != null) {
			setAllowedMimeTypes((String[]) fieldProperties.get("allowedMimeTypes"));
		}
		if (fieldProperties.get("parser") != null) {
			setParserOption(BinaryFieldParserOption.valueOf((String) fieldProperties.get("parser")));
		}
	}

	@Override
	public BinaryFieldParserOption getParserOption() {
		return parser;
	}

	public BinaryFieldSchemaImpl setParserOption(BinaryFieldParserOption parser) {
		this.parser = parser;
		return this;
	}
}
