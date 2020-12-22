package com.gentics.mesh.core.rest.schema.impl;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.BinaryExtractOptions;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

/**
 * @see BinaryFieldSchema
 */
public class BinaryFieldSchemaImpl extends AbstractFieldSchema implements BinaryFieldSchema {

	public static String CHANGE_EXTRACT_CONTENT_KEY = "extractContent";

	public static String CHANGE_EXTRACT_METADATA_KEY = "extractMetadata";

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
		properties.put(SchemaChangeModel.ALLOW_KEY, getAllowedMimeTypes());
		if (binaryExtractOptions == null) {
			properties.put(CHANGE_EXTRACT_CONTENT_KEY, null);
			properties.put(CHANGE_EXTRACT_METADATA_KEY, null);
		} else {
			properties.put(CHANGE_EXTRACT_CONTENT_KEY, binaryExtractOptions.getContent());
			properties.put(CHANGE_EXTRACT_METADATA_KEY, binaryExtractOptions.getMetadata());
		}
		return properties;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		if (fieldProperties.get("allowedMimeTypes") != null) {
			setAllowedMimeTypes((String[]) fieldProperties.get("allowedMimeTypes"));
		}
		boolean hasExtractContent = fieldProperties.get(CHANGE_EXTRACT_CONTENT_KEY) != null;
		if (hasExtractContent) {
			createOrGetBinaryExtractOptions().setContent((Boolean) fieldProperties.get(CHANGE_EXTRACT_CONTENT_KEY));
		}
		boolean hasExtractMetadata = fieldProperties.get(CHANGE_EXTRACT_METADATA_KEY) != null;
		if (hasExtractMetadata) {
			createOrGetBinaryExtractOptions().setMetadata((Boolean) fieldProperties.get(CHANGE_EXTRACT_METADATA_KEY));
		}
		if (!hasExtractContent && !hasExtractMetadata) {
			setBinaryExtractOptions(null);
		}
	}

	private BinaryExtractOptions createOrGetBinaryExtractOptions() {
		if (binaryExtractOptions == null) {
			binaryExtractOptions = new BinaryExtractOptions();
		}
		return binaryExtractOptions;
	}

	@JsonIgnore
	@Override
	public BinaryExtractOptions getBinaryExtractOptions() {
		return binaryExtractOptions;
	}

	@Override
	public BinaryFieldSchema setBinaryExtractOptions(BinaryExtractOptions extract) {
		this.binaryExtractOptions = extract;
		return this;
	}
}
