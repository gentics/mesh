package com.gentics.mesh.plugin.registry;

import com.gentics.mesh.plugin.MeshPlugin;

import io.reactivex.Completable;

/**
 * Interface for a plugin registry. Different plugin types may have different plugin registries. The registry is responsible to hook the plugin into the
 * specific part of Gentics Mesh.
 */
public interface PluginRegistry {

	/**
	 * Register the plugin.
	 * 
	 * @param plugin
	 * @return
	 */
	Completable register(MeshPlugin plugin);

	/**
	 * Deregister the plugin.
	 * 
	 * @param plugin
	 * @return
	 */
	Completable deregister(MeshPlugin plugin);

	/**
	 * Check whether the given plugin conflicts with any already deployed plugin.
	 * 
	 * @param plugin
	 */
	void checkForConflict(MeshPlugin plugin);

}
