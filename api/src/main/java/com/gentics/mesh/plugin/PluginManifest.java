package com.gentics.mesh.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;

/**
 * The plugin manifest describes a plugin and is also used to setup the path to the REST API.
 */
public class PluginManifest {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Unique id of the plugin was defined by the plugin developer.")
	private String id;

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

	public String getId() {
		return id;
	}

	@Setter
	public PluginManifest setId(String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	@Setter
	public PluginManifest setName(String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	@Setter
	public PluginManifest setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getVersion() {
		return version;
	}

	@Setter
	public PluginManifest setVersion(String version) {
		this.version = version;
		return this;
	}

	public String getAuthor() {
		return author;
	}

	@Setter
	public PluginManifest setAuthor(String author) {
		this.author = author;
		return this;
	}

	public String getInception() {
		return inception;
	}

	@Setter
	public PluginManifest setInception(String inception) {
		this.inception = inception;
		return this;
	}

	public String getLicense() {
		return license;
	}

	@Setter
	public PluginManifest setLicense(String license) {
		this.license = license;
		return this;
	}

	/**
	 * Return a new empty manifest.
	 * 
	 * @return
	 */
	public static PluginManifest manifest() {
		return new PluginManifest();
	}
}
