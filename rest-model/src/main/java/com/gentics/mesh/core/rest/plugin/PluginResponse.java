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
	@JsonPropertyDescription("Deployment UUUID of the plugin. Note that each deployment will get a new UUID.")
	private String uuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the plugin.")
	private String name;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Manifest of the plugin")
	private PluginManifest manifest;

	public PluginResponse() {
	}

	/**
	 * Return the Vert.x deployment Uuid of the plugin.
	 * 
	 * @return
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the plugin verticle deployment Uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	public PluginResponse setUuid(String uuid) {
		this.uuid = uuid;
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
}
