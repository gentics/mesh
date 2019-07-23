package com.gentics.mesh.plugin;

import org.pf4j.PluginDescriptor;

public interface MeshPluginDescriptor extends PluginDescriptor {

	/**
	 * Return the id of the plugin.
	 * 
	 * @return
	 */
	String getId();

	/**
	 * Return name of the plugin.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Return the plugin uuid.
	 * 
	 * @return
	 */
	String getUuid();

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
	 * Transform the descriptor to a mesh plugin manifest.
	 * 
	 * @return
	 */
	PluginManifest toPluginManifest();

}
