package com.gentics.mesh.plugin.env;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.Vertx;

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

	/**
	 * Return the Vert.x instance to be used for the plugin.
	 * 
	 * @return
	 */
	Vertx vertx();

	/**
	 * Create a new admin client.
	 * 
	 * @return
	 */
	MeshRestClient createAdminClient();

	/**
	 * Return the Mesh options.
	 * 
	 * @return
	 */
	MeshOptions options();
}
