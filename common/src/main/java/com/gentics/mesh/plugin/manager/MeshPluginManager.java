package com.gentics.mesh.plugin.manager;

import java.io.File;
import java.util.Map;

import org.pf4j.Plugin;
import org.pf4j.PluginManager;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.PluginManifest;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * The plugin manager can be used to deploy plugins and register them in Gentics Mesh.
 */
public interface MeshPluginManager extends PluginManager {

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
	 * @return
	 */
	Single<String> deploy(AbstractPlugin plugin);

	/**
	 * Deploy the plugin file.
	 * 
	 * @param file
	 * @return Single which contains the deployment id
	 */
	Single<String> deploy(File file);

	/**
	 * Deploy the plugin with the given service name.
	 * 
	 * @param name
	 * @return Single which contains the plugin deployment uuid.
	 */
	Single<String> deploy(String name);

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
	 * @param strict
	 * @return
	 */
	Completable validate(MeshPlugin plugin, boolean strict);

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
	 * Return a map of all deployed plugins.
	 * 
	 * @return
	 */
	Map<String, MeshPlugin> getPluginsMap();

	/**
	 * Add the given plugin and register it.
	 * 
	 * @param plugin
	 * @return
	 */
	String addPlugin(Plugin plugin);

	/**
	 * Deploy the plugin via the given class.
	 * 
	 * @param clazz
	 * @param pluginId
	 * @return
	 */
	Single<String> deploy(Class<?> clazz, String pluginId);
}
