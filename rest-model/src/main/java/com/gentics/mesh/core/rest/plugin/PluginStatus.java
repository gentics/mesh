package com.gentics.mesh.core.rest.plugin;

public enum PluginStatus {

	/**
	 * Plugin jar is loaded
	 */
	LOADED,

	/**
	 * Plugin is validated
	 */
	VALIDATED,
	
	/**
	 * Loaded plugin is started
	 */
	STARTED,

	/**
	 * Plugin is pre-registered and will be registered and initialized once mesh is ready
	 */
	PRE_REGISTERED,

	/**
	 * Plugin is initialized
	 */
	INITIALIZED,

	/**
	 * Plugin is registered and API endpoints are accessible.
	 */
	REGISTERED,

	/**
	 * The plugin is in a failed state.
	 */
	FAILED,

	/**
	 * The plugin is in a failed state, but will be retried when the storages become available again.
	 */
	FAILED_RETRY;

}
