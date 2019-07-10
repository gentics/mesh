package com.gentics.mesh.plugin;

import org.pf4j.PluginDescriptor;

public interface MeshPluginDescriptor extends PluginDescriptor {

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

	/**
	 * Return name of the plugin.
	 * @return
	 */
	String getName();

}
