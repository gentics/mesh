package com.gentics.mesh.plugin.env;

/**
 * Environment for a plugin is used to access data provided by mesh (e.g. adminToken).
 */
public interface PluginEnvironment {

	/**
	 * Return an admin API token.
	 * 
	 * @return
	 */
	String adminToken();

}
