package com.gentics.mesh.core.rest.node.field.binary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Model for requests to a binary check service.
 */
public class BinaryCheckRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The checked binary's filename")
	private String filename;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The checked binary's MIME type")
	private String mimeType;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The URL for the check service to download the binary.")
	private String downloadUrl;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The URL where the check service should post its results to.")
	private String callbackUrl;

	/**
	 * Get the binary's filename.
	 * @return The binary's filename.
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Set the binary's filename.
	 * @param filename The binary's filename.
	 * @return Fluent API.
	 */
	public BinaryCheckRequest setFilename(String filename) {
		this.filename = filename;
		return this;
	}

	/**
	 * Get the binary's MIME type.
	 * @return The binary's MIME type.
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Set the binary's MIME type.
	 * @param mimeType The binary's MIME type.
	 * @return Fluent API.
	 */
	public BinaryCheckRequest setMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	/**
	 * Get the binary's download URL.
	 * @return The binary's download URL.
	 */
	public String getDownloadUrl() {
		return downloadUrl;
	}

	/**
	 * Set the binary's download URL.
	 * @param downloadUrl The binary's download URL.
	 * @return Fluent API.
	 */
	public BinaryCheckRequest setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
		return this;
	}

	/**
	 * Get the callback URL for the check service.
	 * @return The callback URL for the check service.
	 */
	public String getCallbackUrl() {
		return callbackUrl;
	}

	/**
	 * Set the callback URL for the check service.
	 * @param callbackUrl The callback URL for the check service.
	 * @return Fluent API.
	 */
	public BinaryCheckRequest setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
		return this;
	}
}
