package com.gentics.mesh.plugin.gcms.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;
import com.gentics.mesh.plugin.common.license.LicenseConfiguration;

/**
 * Configuration POJO
 */
public class GCMSPluginConfig extends LicenseConfiguration {
	public final static String PUBLIC_KEY_PATH_ENV = "MESH_GCMS_PLUGIN_PUBLIC_KEY_PATH";

	public final static String ISSUER_CONFIG_ENV = "MESH_GCMS_PLUGIN_ISSUER";

	@JsonProperty(defaultValue = "public-key.jwk")
	@JsonPropertyDescription("Path to the JWK file containing the public key")
	@EnvironmentVariable(name = PUBLIC_KEY_PATH_ENV, description = "Override the path to the public key")
	private String publicKeyPath = "public-key.jwk";

	@JsonProperty(defaultValue = "Gentics CMS")
	@JsonPropertyDescription("Value the iss claim (issuer claim) must have for the mapping to be done")
	@EnvironmentVariable(name = ISSUER_CONFIG_ENV, description = "Override the expected issuer claim")
	private String issuer = "Gentics CMS";

	public String getPublicKeyPath() {
		return publicKeyPath;
	}

	public GCMSPluginConfig setPublicKeyPath(String publicKeyPath) {
		this.publicKeyPath = publicKeyPath;
		return this;
	}

	public String getIssuer() {
		return issuer;
	}

	public GCMSPluginConfig setIssuer(String issuer) {
		this.issuer = issuer;
		return this;
	}

	/**
	 * Validate configuration
	 */
	@JsonIgnore
	public void validate() {
	}
}
