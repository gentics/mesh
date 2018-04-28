package com.gentics.mesh.core.rest.plugin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * The plugin manifest describes a plugin and is also used to setup the path to the REST API.
 */
public class PluginManifest {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Human readable name of the plugin.")
	private String name;

	@JsonProperty(required = true)
	@JsonPropertyDescription("API name of the plugin. This will be used to construct the REST API path to the plugin.")
	private String apiName;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Description of the plugin.")
	private String description;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Version of the plugin.")
	private String version;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Author of the plugin.")
	private String author;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Inception date of the plugin.")
	private String inception;

	@JsonProperty(required = true)
	@JsonPropertyDescription("License of the plugin.")
	private String license;

	public PluginManifest() {
	}

	public String getName() {
		return name;
	}

	public PluginManifest setName(String name) {
		this.name = name;
		return this;
	}

	public String getApiName() {
		return apiName;
	}

	public PluginManifest setApiName(String apiName) {
		this.apiName = apiName;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public PluginManifest setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public PluginManifest setVersion(String version) {
		this.version = version;
		return this;
	}

	public String getAuthor() {
		return author;
	}

	public PluginManifest setAuthor(String author) {
		this.author = author;
		return this;
	}

	public String getInception() {
		return inception;
	}

	public PluginManifest setInception(String inception) {
		this.inception = inception;
		return this;
	}

	public String getLicense() {
		return license;
	}

	public PluginManifest setLicense(String license) {
		this.license = license;
		return this;
	}

	/**
	 * Validate the manifest.
	 * 
	 * @return
	 */
	public PluginManifest validate() {
		if (StringUtils.isEmpty(name)) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "name");
		}
		if (StringUtils.isEmpty(apiName)) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "apiName");
		}
		if (apiName.contains(" ") | apiName.contains("/")) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_apiname_invalid", name);
		}
		if (StringUtils.isEmpty(version)) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "version");
		}
		if (StringUtils.isEmpty(author)) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "author");
		}
		if (StringUtils.isEmpty(license)) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "license");
		}
		if (StringUtils.isEmpty(inception)) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "inception");
		}
		if (StringUtils.isEmpty(description)) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "description");
		}
		if (StringUtils.isEmpty(version)) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "version");
		}
		return this;
	}
}
