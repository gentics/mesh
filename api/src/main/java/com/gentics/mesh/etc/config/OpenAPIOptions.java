package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.etc.config.Format;
import com.gentics.mesh.etc.config.Version;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * OpenAPI related options
 */
public class OpenAPIOptions implements Option {

	public static final String MESH_OPENAPI_EXCLUDE_PLUGINS_ENV = "MESH_OPENAPI_EXCLUDE_PLUGINS";
	public static final String MESH_OPENAPI_NO_INFO_PLUGINS_ENV = "MESH_OPENAPI_NO_INFO_PLUGINS";
	public static final String MESH_OPENAPI_DEFAULT_VERSION_ENV = "MESH_OPENAPI_DEFAULT_VERSION";
	public static final String MESH_OPENAPI_DEFAULT_FORMAT_ENV = "MESH_OPENAPI_DEFAULT_FORMAT";

	public static final Version DEFAULT_MESH_OPENAPI_DEFAULT_VERSION = Version.V30;
	public static final Format DEFAULT_MESH_OPENAPI_DEFAULT_FORMAT = Format.YAML;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Should the endpoints of the included plugins be excluded from the OpenAPI specification generation requests. Default is true.")
	@EnvironmentVariable(name = MESH_OPENAPI_EXCLUDE_PLUGINS_ENV, description = "Override the flag to exclude endpoints of the plugins from the OpenAPI specification generation requests")
	private boolean excludePlugins = true;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Comma separated list of plugins, that should provide no API info. Default is null")
	@EnvironmentVariable(name = MESH_OPENAPI_NO_INFO_PLUGINS_ENV, description = "Override the list of Mesh Plugins with no API info")
	private String noApiInfoPlugins = null;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Default OpenAPI specification version. Default value: V30")
	@EnvironmentVariable(name = MESH_OPENAPI_DEFAULT_VERSION_ENV, description = "Override the default OpenAPI specification version")
	private Version defaultVersion = DEFAULT_MESH_OPENAPI_DEFAULT_VERSION;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Default OpenAPI specification format. Default value: YAML")
	@EnvironmentVariable(name = MESH_OPENAPI_DEFAULT_FORMAT_ENV, description = "Override the default OpenAPI specification format")
	private Format defaultFormat = DEFAULT_MESH_OPENAPI_DEFAULT_FORMAT;

	public boolean isExcludePlugins() {
		return excludePlugins;
	}

	@Setter
	public OpenAPIOptions setExcludePlugins(boolean openapiExcludePlugins) {
		this.excludePlugins = openapiExcludePlugins;
		return this;
	}

	public String getNoApiInfoPlugins() {
		return noApiInfoPlugins;
	}

	@Setter
	public OpenAPIOptions setNoApiInfoPlugins(String noApiInfoPlugins) {
		this.noApiInfoPlugins = noApiInfoPlugins;
		return this;
	}

	public Version getDefaultVersion() {
		return defaultVersion;
	}

	@Setter
	public OpenAPIOptions setDefaultVersion(Version defaultVersion) {
		this.defaultVersion = defaultVersion;
		return this;
	}

	public Format getDefaultFormat() {
		return defaultFormat;
	}

	@Setter
	public OpenAPIOptions setDefaultFormat(Format defaultFormat) {
		this.defaultFormat = defaultFormat;
		return this;
	}
}
