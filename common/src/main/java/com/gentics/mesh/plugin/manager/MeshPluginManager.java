package com.gentics.mesh.plugin.manager;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.MeshPlugin;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * The plugin manager can be used to deploy plugins and register them in Gentics Mesh.
 */
public interface MeshPluginManager {

	/**
	 * Initialize the plugin manager.
	 * 
	 * @param options
	 */
	void init(MeshOptions options);

	/**
	 * Deploy the plugin with the given path.
	 * 
	 * @param path
	 * @return Single which contains the plugin deployment uuid.
	 */
	Single<String> deploy(Path path);

	/**
	 * Deploy the plugin file with the given name. The extension will be added automatically.
	 * 
	 * @param pluginName
	 * @return
	 */
	default Single<String> deploy(String pluginName) {
		return deploy(getPluginsRoot().resolve(pluginName + ".jar"));
	}

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
	 * Deploy the plugin via the given class.
	 * 
	 * @param clazz
	 * @param pluginId
	 * @return
	 */
	Single<String> deploy(Class<?> clazz, String pluginId);

	/**
	 * Unloads all currently loaded plugins.
	 */
	void unloadPlugins();

	/**
	 * Return the pluginIds of all loaded plugins.
	 * 
	 * @return
	 */
	Set<String> getPluginIds();

	/**
	 * Fetch the plugin using the given id.
	 * 
	 * @param pluginId
	 * @return
	 */
	PluginWrapper getPlugin(String pluginId);

	Path getPluginsRoot();

	Map<String, String> pluginIdsMap();

}
