package com.gentics.mesh.plugin.common.license;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

public class LicenseConfiguration implements Option {

	private static final String LICENSE_KEY_ENV = "LICENSEKEY";

	@JsonProperty(defaultValue = "config/license.key")
	@JsonPropertyDescription("Path to the license key file.")
	@EnvironmentVariable(name = LICENSE_KEY_ENV, description = "Override the default licence key file.")
	private String licenseKey = "config/license.key";

	public String getLicenseKey() {
		return licenseKey;
	}

	public void setLicenseKey(String licenseKey) {
		this.licenseKey = licenseKey;
	}
}
