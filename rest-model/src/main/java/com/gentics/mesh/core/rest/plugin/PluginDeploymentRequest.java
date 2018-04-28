package com.gentics.mesh.core.rest.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Deployment request for a plugin.
 */
public class PluginDeploymentRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Deployment name of the plugin. This can either be a filesystem or maven deployment.")
	private String name;

	public PluginDeploymentRequest() {
	}

	/**
	 * Return the deployment name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the deployment name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public PluginDeploymentRequest setName(String name) {
		this.name = name;
		return this;
	}
}
