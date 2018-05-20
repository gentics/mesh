package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

import io.vertx.core.json.JsonObject;

public class OAuth2Options implements Option {

	public static final String MESH_AUTH_OAUTH2_ENABLED_ENV = "MESH_AUTH_OAUTH2_ENABLED";

	public static final String MESH_AUTH_OAUTH2_MAPPER_SCRIPT_PATH_ENV = "MESH_AUTH_OAUTH2_MAPPER_SCRIPT_PATH";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the OAuth2 support should be enabled.")
	@EnvironmentVariable(name = MESH_AUTH_OAUTH2_ENABLED_ENV, description = "Override the configured OAuth2 enabled flag.")
	private boolean enabled = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Property which contains the OAuth2 configuration settings like realm name, auth server url.")
	private JsonObject config = null;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the OAuth2 mapper script.")
	@EnvironmentVariable(name = MESH_AUTH_OAUTH2_MAPPER_SCRIPT_PATH_ENV, description = "Override the configured OAuth2 mapper script path.")
	private String mapperScriptPath = null;

	public boolean isEnabled() {
		return enabled;
	}

	public OAuth2Options setEnabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public JsonObject getConfig() {
		return config;
	}

	public OAuth2Options setConfig(JsonObject config) {
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

}
