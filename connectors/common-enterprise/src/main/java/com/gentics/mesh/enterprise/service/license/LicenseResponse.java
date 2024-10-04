package com.gentics.mesh.enterprise.service.license;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

public class LicenseResponse extends AbstractGenericRestResponse {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The licenseKey, obfuscated.")
	private String licenseKey;

	@JsonProperty(required = true)
	@JsonPropertyDescription("License status.")
	private String status;

	@JsonProperty(required = true)
	@JsonPropertyDescription("License features.")
	private Set<String> features;

	public LicenseResponse setLicenseKey(String key) {
		this.licenseKey = key;
		return this;
	}

	public LicenseResponse setStatus(String name) {
		this.status = name;
		return this;
	}

	public LicenseResponse setFeatures(Set<String> features) {
		this.features = features;
		return this;
	}

	public String getLicenseKey() {
		return licenseKey;
	}

	public String getStatus() {
		return status;
	}

	public Set<String> getFeatures() {
		return features;
	}
}
