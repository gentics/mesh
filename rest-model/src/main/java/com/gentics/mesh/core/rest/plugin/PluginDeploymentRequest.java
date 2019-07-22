package com.gentics.mesh.core.rest.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Deployment request for a plugin.
 */
public class PluginDeploymentRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Deployment path of the plugin which is relative to the plugin directory.")
	private String path;

	public PluginDeploymentRequest() {
	}

	/**
	 * Return the deployment path.
	 * 
	 * @return
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the deployment path.
	 * 
	 * @param path
	 * @return Fluent API
	 */
	public PluginDeploymentRequest setPath(String path) {
		this.path = path;
		return this;
	}
}
