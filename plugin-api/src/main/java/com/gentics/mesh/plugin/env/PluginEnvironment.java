package com.gentics.mesh.plugin.env;

import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.RestAPIVersion;
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
	 * Create a new admin client with api version 1.
	 *
	 * @return A new MeshRestClient instance with the Admin Access Token set in the API version v1
	 */
	MeshRestClient createAdminClient();

	/**
	 * Create a new rest client.
	 * 
	 * @param token Token to be set in the client
	 * @return
	 */
	MeshRestClient createClient(String token);

	/**
	 * Creates a new admin client for the specified version.
	 *
	 * @param version
	 * 		The version which the newly created client should interact with
	 *
	 * @return A new MeshRestClient instance with the Admin Access Token set in the specified API version
	 */
	MeshRestClient createAdminClient(RestAPIVersion version);

	/**
	 * Return the Mesh options.
	 *
	 * @return
	 */
	AbstractMeshOptions options();

}
