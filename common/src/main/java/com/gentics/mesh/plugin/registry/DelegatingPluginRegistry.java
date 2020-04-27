package com.gentics.mesh.plugin.registry;

import com.gentics.mesh.plugin.MeshPlugin;

import io.reactivex.Completable;

public interface DelegatingPluginRegistry {

	default void start() {

	}

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
