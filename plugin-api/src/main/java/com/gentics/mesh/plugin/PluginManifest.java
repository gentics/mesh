package com.gentics.mesh.plugin;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class PluginManifest {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the plugin.")
	private String name;

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

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getInception() {
		return inception;
	}

	public void setInception(String inception) {
		this.inception = inception;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public void validate() {
		Objects.requireNonNull(name, "The name of the plugin must be specified.");
		Objects.requireNonNull(version, "The version of the plugin must be specified.");
		Objects.requireNonNull(author, "The author of the plugin must be specified.");
		Objects.requireNonNull(license, "The license of the plugin must be specified.");
		Objects.requireNonNull(inception, "The inception date of the plugin must be specifed");
		Objects.requireNonNull(description, "The description of the plugin must be specified.");
	}
}
