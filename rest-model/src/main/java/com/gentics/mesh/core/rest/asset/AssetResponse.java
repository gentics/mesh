package com.gentics.mesh.core.rest.asset;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

public class AssetResponse extends AbstractGenericRestResponse {

	@JsonPropertyDescription("Filename of the asset.")
	private String filename;

	@JsonPropertyDescription("SHA512 checksum of the binary data.")
	private String sha512sum;

	public String getFilename() {
		return filename;
	}

	public AssetResponse setFilename(String filename) {
		this.filename = filename;
		return this;
	}

	public String getSha512sum() {
		return sha512sum;
	}

	public AssetResponse setSha512sum(String sha512sum) {
		this.sha512sum = sha512sum;
		return this;
	}
}
