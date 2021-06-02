package com.gentics.mesh.core.rest.node.field.s3binary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a s3 binary field upload request
 */
public class S3BinaryUploadRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Version number which must be provided in order to handle and detect concurrent changes to the node content.")
	private String version;

	@JsonProperty(required = true)
	@JsonPropertyDescription("ISO 639-1 language tag of the node which provides the image which should be transformed.")
	private String language;

	@JsonPropertyDescription("Expected filename for the s3 binary field.")
	private String filename;

	public String getVersion() {
		return version;
	}

	public S3BinaryUploadRequest setVersion(String version) {
		this.version = version;
		return this;
	}

	public String getLanguage() {
		return language;
	}

	public S3BinaryUploadRequest setLanguage(String language) {
		this.language = language;
		return this;
	}

	public String getFilename() {
		return filename;
	}

	public S3BinaryUploadRequest setFilename(String filename) {
		this.filename = filename;
		return this;
	}
}
