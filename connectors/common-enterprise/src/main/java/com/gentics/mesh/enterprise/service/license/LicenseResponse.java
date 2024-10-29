package com.gentics.mesh.enterprise.service.license;

import java.time.Instant;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

public class LicenseResponse extends AbstractGenericRestResponse {

	@JsonProperty(required = false)
	@JsonPropertyDescription("The licenseKey, obfuscated.")
	private String licenseKey;

	@JsonProperty(required = true)
	@JsonPropertyDescription("License status.")
	private String status;

	@JsonProperty(required = false)
	@JsonPropertyDescription("License features.")
	private Set<String> features;

	@JsonProperty(required = false)
	@JsonPropertyDescription("License issued at.")
	private Instant issuedAt;

	@JsonProperty(required = false)
	@JsonPropertyDescription("License expires at.")
	private Instant expiresAt;

	@JsonProperty(required = false)
	@JsonPropertyDescription("License issuer.")
	private String issuer;

	@JsonProperty(required = false)
	@JsonPropertyDescription("License subject.")
	private String subject;

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

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public LicenseResponse setIssuedAt(Instant issuedAt) {
		this.issuedAt = issuedAt;
		return this;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public LicenseResponse setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
		return this;
	}

	public String getIssuer() {
		return issuer;
	}

	public LicenseResponse setIssuer(String issuer) {
		this.issuer = issuer;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public LicenseResponse setSubject(String subject) {
		this.subject = subject;
		return this;
	}
}
