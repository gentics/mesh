package com.gentics.mesh.core.rest.schema.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.S3BinaryExtractOptions;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

import java.util.Map;

/**
 * @see S3BinaryFieldSchema
 */
public class S3BinaryFieldSchemaImpl extends AbstractFieldSchema implements S3BinaryFieldSchema {

	public static String CHANGE_EXTRACT_CONTENT_KEY = "extractContent";

	public static String CHANGE_EXTRACT_METADATA_KEY = "extractMetadata";

	@JsonProperty("allow")
	@JsonPropertyDescription("Array of allowed mimetypes")
	private String[] allowedMimeTypes;

	@JsonProperty("extract")
	@JsonPropertyDescription("The extracting behaviour for this field.")
	private S3BinaryExtractOptions s3binaryExtractOptions;

	@Override
	public String[] getAllowedMimeTypes() {
		return allowedMimeTypes;
	}

	@Override
	public S3BinaryFieldSchema setAllowedMimeTypes(String... allowedMimeTypes) {
		this.allowedMimeTypes = allowedMimeTypes;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.S3BINARY.toString();
	}

	@Override
	public Map<String, Object> getAllChangeProperties() {
		Map<String, Object> properties = super.getAllChangeProperties();
		properties.put(SchemaChangeModel.ALLOW_KEY, getAllowedMimeTypes());
		if (s3binaryExtractOptions == null) {
			properties.put(CHANGE_EXTRACT_CONTENT_KEY, null);
			properties.put(CHANGE_EXTRACT_METADATA_KEY, null);
		} else {
			properties.put(CHANGE_EXTRACT_CONTENT_KEY, s3binaryExtractOptions.getContent());
			properties.put(CHANGE_EXTRACT_METADATA_KEY, s3binaryExtractOptions.getMetadata());
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
			setS3BinaryExtractOptions(null);
		}
	}

	private S3BinaryExtractOptions createOrGetBinaryExtractOptions() {
		if (s3binaryExtractOptions == null) {
			s3binaryExtractOptions = new S3BinaryExtractOptions();
		}
		return s3binaryExtractOptions;
	}

	@JsonIgnore
	@Override
	public S3BinaryExtractOptions getS3BinaryExtractOptions() {
		return s3binaryExtractOptions;
	}

	@Override
	public S3BinaryFieldSchema setS3BinaryExtractOptions(S3BinaryExtractOptions extract) {
		this.s3binaryExtractOptions = extract;
		return this;
	}
}