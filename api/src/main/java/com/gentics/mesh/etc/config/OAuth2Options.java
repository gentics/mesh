package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

public class OAuth2Options implements Option {

	public static final String MESH_AUTH_OAUTH2_ENABLED_ENV = "MESH_AUTH_OAUTH2_ENABLED";

	public static final String MESH_AUTH_OAUTH2_MAPPER_SCRIPT_PATH_ENV = "MESH_AUTH_OAUTH2_MAPPER_SCRIPT_PATH";

	public static final String MESH_AUTH_OAUTH2_MAPPER_SCRIPT_DEV_MODE_ENV = "MESH_AUTH_OAUTH2_MAPPER_SCRIPT_DEV_MODE";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the OAuth2 support should be enabled.")
	@EnvironmentVariable(name = MESH_AUTH_OAUTH2_ENABLED_ENV, description = "Override the configured OAuth2 enabled flag.")
	private boolean enabled = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Property which contains the OAuth2 configuration settings like realm name, auth server url.")
	private OAuth2ServerConfig config = null;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the OAuth2 mapper script.")
	@EnvironmentVariable(name = MESH_AUTH_OAUTH2_MAPPER_SCRIPT_PATH_ENV, description = "Override the configured OAuth2 mapper script path.")
	private String mapperScriptPath = null;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the OAuth2 mapper script development mode should be enabled. The mapper script will be re-loaded for each request when enabled.")
	@EnvironmentVariable(name = MESH_AUTH_OAUTH2_MAPPER_SCRIPT_DEV_MODE_ENV, description = "Override the configured OAuth2 mapper script development mode flag.")
	private boolean mapperScriptDevMode = false;

	public boolean isEnabled() {
		return enabled;
	}

	public OAuth2Options setEnabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public OAuth2ServerConfig getConfig() {
		return config;
	}

	public OAuth2Options setConfig(OAuth2ServerConfig config) {
		this.config = config;
		return this;
	}

	public String getMapperScriptPath() {
		return mapperScriptPath;
	}

	public OAuth2Options setMapperScriptPath(String mapperScriptPath) {
		this.mapperScriptPath = mapperScriptPath;
		return this;
	}

	public boolean isMapperScriptDevMode() {
		return mapperScriptDevMode;
	}

	public OAuth2Options setMapperScriptDevMode(boolean mapperScriptDevMode) {
		this.mapperScriptDevMode = mapperScriptDevMode;
		return this;
	}
}
