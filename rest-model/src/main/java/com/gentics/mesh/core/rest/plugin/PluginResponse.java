package com.gentics.mesh.core.rest.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.plugin.PluginManifest;

/**
 * Response which contains information about a deployed plugin.
 */
public class PluginResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Id of the plugin.")
	private String id;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the plugin.")
	private String name;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Manifest of the plugin")
	private PluginManifest manifest;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Status of the plugin")
	private PluginStatus status;

	public PluginResponse() {
	}

	/**
	 * Return the id of the plugin.
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the plugin Id.
	 * 
	 * @param id
	 * @return
	 */
	public PluginResponse setId(String id) {
		this.id = id;
		return this;
	}

	/**
	 * Return the plugin name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the plugin.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public PluginResponse setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Return the plugin manifest.
	 * 
	 * @return
	 */
	public PluginManifest getManifest() {
		return manifest;
	}

	/**
	 * Set the plugin manifest.
	 * 
	 * @param manifest
	 * @return
	 */
	public PluginResponse setManifest(PluginManifest manifest) {
		this.manifest = manifest;
		return this;
	}

	public PluginStatus getStatus() {
		return status;
	}

	public PluginResponse setStatus(PluginStatus status) {
		this.status = status;
		return this;
	}

}
