package com.gentics.mesh.plugin;

import java.io.File;
import java.util.Map;

import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * The plugin manager can be used to deploy plugins and register them in Gentics Mesh.
 */
public interface PluginManager {

	/**
	 * Initialize the plugin manager.
	 * 
	 * @param options
	 */
	void init(MeshOptions options);

	/**
	 * Deploy the given plugin.
	 * 
	 * @param plugin
	 */
	Single<String> deploy(MeshPlugin plugin);

	/**
	 * Deploy the plugin file.
	 * 
	 * @param file
	 * @return Single which contains the deployment id
	 */
	Single<String> deploy(File file);

	/**
	 * Register the given plugin.
	 * 
	 * @param plugin
	 * @return
	 */
	Completable registerPlugin(MeshPlugin plugin);

	/**
	 * de-register the given plugin.
	 * 
	 * @param plugin
	 */
	Completable deregisterPlugin(MeshPlugin plugin);

	/**
	 * Find the plugin with the given uuid and return it.
	 * 
	 * @param uuid
	 * @return
	 */
	MeshPlugin getPlugin(String uuid);

	/**
	 * Deploy the plugin with the given service name.
	 * 
	 * @param name
	 * @return Single which contains the plugin deployment uuid.
	 */
	Single<String> deploy(String name);

	/**
	 * Return a map of all deployed plugins.
	 * 
	 * @return
	 */
	Map<String, MeshPlugin> getPlugins();

	/**
	 * Undeploy and de-register the plugin with the given uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	Completable undeploy(String uuid);

	/**
	 * Validate the plugin and ensure that the plugin can only be deployed once.
	 * 
	 * @param plugin
	 * @return
	 */
	Completable validate(MeshPlugin plugin);

	/**
	 * Stop the manager and undeploy all currently deployed plugins.
	 * 
	 * @return
	 */
	Completable stop();

	/**
	 * Deploy plugins which are located in the plugin directory.
	 */
	Completable deployExistingPluginFiles();

	/**
	 * Return an admin API token.
	 * 
	 * @return
	 */
	String adminToken();

}
