package com.gentics.mesh.plugin.registry;

import com.gentics.mesh.plugin.MeshPlugin;

import io.reactivex.Completable;

/**
 * Plugin registry which can delegate plugin registrations to registries for the dedicated sections (e.g. graphql registry for graphql plugins).
 */
public interface DelegatingPluginRegistry {

	/**
	 * Method will be invoked when the registry is first used.
	 */
	default void start() {

	}

	/**
	 * Method will be invoked to unload all plugins.
	 */
	default void stop() {

	}

	/**
	 * Pre-register the plugin. The pre-registered plugins will be register when {@link #register()} get invoked.
	 * 
	 * @param plugin
	 */
	void preRegister(MeshPlugin plugin);

	/**
	 * Check whether any other plugin already occupies the api name of the given plugin.
	 * 
	 * @param plugin
	 */
	void checkForConflict(MeshPlugin plugin);

	/**
	 * Deregister the plugin. This will also delegate the de-registration.
	 * 
	 * @param plugin
	 * @return
	 */
	Completable deregister(MeshPlugin plugin);

}
