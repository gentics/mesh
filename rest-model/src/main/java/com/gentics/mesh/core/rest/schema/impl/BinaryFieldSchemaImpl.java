package com.gentics.mesh.core.rest.schema.impl;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.BinaryExtractOptions;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;

public class BinaryFieldSchemaImpl extends AbstractFieldSchema implements BinaryFieldSchema {

	@JsonProperty("allow")
	@JsonPropertyDescription("Array of allowed mimetypes")
	private String[] allowedMimeTypes;

	@JsonProperty("extract")
	@JsonPropertyDescription("The extracting behaviour for this field.")
	private BinaryExtractOptions binaryExtractOptions;

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
		if (binaryExtractOptions == null) {
			properties.put("extractContent", null);
			properties.put("extractMetadata", null);
		} else {
			properties.put("extractContent", binaryExtractOptions.getContent());
			properties.put("extractMetadata", binaryExtractOptions.getMetadata());
		}
		return properties;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		if (fieldProperties.get("allowedMimeTypes") != null) {
			setAllowedMimeTypes((String[]) fieldProperties.get("allowedMimeTypes"));
		}
		if (fieldProperties.get("extractContent") != null) {
			createOrGetBinaryExtractOptions().setContent((Boolean)fieldProperties.get("extractContent"));
		}
		if (fieldProperties.get("extractMetadata") != null) {
			createOrGetBinaryExtractOptions().setMetadata((Boolean)fieldProperties.get("extractMetadata"));
		}
	}

	private BinaryExtractOptions createOrGetBinaryExtractOptions() {
		if (binaryExtractOptions == null) {
			binaryExtractOptions = new BinaryExtractOptions();
		}
		return binaryExtractOptions;
	}

	@JsonIgnore
	public BinaryExtractOptions getBinaryExtractOptions() {
		return binaryExtractOptions;
	}

	public BinaryFieldSchemaImpl setBinaryExtractOptions(BinaryExtractOptions extract) {
		this.binaryExtractOptions = extract;
		return this;
	}
}
