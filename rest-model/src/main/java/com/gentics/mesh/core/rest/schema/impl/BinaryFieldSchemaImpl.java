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

	public static String CHANGE_CHECK_SERVICE_URL = "checkServiceUrl";

	@JsonProperty("allow")
	@JsonPropertyDescription("Array of allowed mimetypes")
	private String[] allowedMimeTypes;

	@JsonProperty("extract")
	@JsonPropertyDescription("The extracting behaviour for this field.")
	private BinaryExtractOptions binaryExtractOptions;

	@JsonProperty("checkServiceUrl")
	@JsonPropertyDescription("The URL for the optional service which checks uploaded binaries before making them available.")
	private String checkServiceUrl;

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

		properties.put(CHANGE_CHECK_SERVICE_URL, checkServiceUrl);

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

		if (fieldProperties.get(CHANGE_CHECK_SERVICE_URL) != null) {
			setCheckServiceUrl((String) fieldProperties.get(CHANGE_CHECK_SERVICE_URL));
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

	@Override
	public String getCheckServiceUrl() {
		return checkServiceUrl;
	}

	@Override
	public BinaryFieldSchemaImpl setCheckServiceUrl(String checkServiceUrl) {
		this.checkServiceUrl = checkServiceUrl;
		return this;
	}
}
