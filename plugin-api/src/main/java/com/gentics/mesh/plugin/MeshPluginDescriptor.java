package com.gentics.mesh.plugin;

import org.pf4j.PluginDescriptor;

import com.gentics.mesh.core.rest.plugin.PluginManifest;

public interface MeshPluginDescriptor extends PluginDescriptor {

	/**
	 * Return the API name for the plugin.
	 * 
	 * @return
	 */
	String getAPIName();

	/**
	 * Transform the descriptor to a mesh plugin manifest.
	 * 
	 * @return
	 */
	PluginManifest toPluginManifest();

	/**
	 * Return the author of the plugin.
	 * 
	 * @return
	 */
	String getAuthor();

	/**
	 * Return the inception date of the plugin.
	 * 
	 * @return
	 */
	String getInception();

}
