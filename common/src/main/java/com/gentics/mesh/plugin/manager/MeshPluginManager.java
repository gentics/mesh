package com.gentics.mesh.plugin.manager;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.MeshPlugin;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * The plugin manager can be used to deploy plugins and register them in Gentics Mesh.
 */
public interface MeshPluginManager {

	/**
	 * Initialize the plugin manager.
	 */
	void init();

	/**
	 * Deploy the plugin with the given path.
	 * 
	 * @param path
	 * @return Single which contains the plugin deployment id.
	 */
	Single<String> deploy(Path path);

	/**
	 * Deploy the plugin file with the given path. The path must be relative to the plugin directory.
	 * 
	 * @param path
	 * @return
	 */
	default Single<String> deploy(String path) {
		return deploy(getPluginsRoot().resolve(path));
	}

	/**
	 * Undeploy and de-register the plugin with the given id.
	 * 
	 * @param id
	 * @return
	 */
	Completable undeploy(String id);

	/**
	 * Validate the plugin and ensure that the plugin can only be deployed once.
	 * 
	 * @param plugin
	 * @return
	 */
	void validate(Plugin plugin);

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
	 * @param id
	 *            The id of the plugin (e.g. hello-world)
	 * @return
	 */
	Completable deploy(Class<?> clazz, String id);

	/**
	 * Unloads all currently loaded plugins.
	 */
	void unloadPlugins();

	/**
	 * Return the IDs of all started plugins.
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

	/**
	 * Return the path to the plugins root directory.
	 * 
	 * @return
	 */
	Path getPluginsRoot();

	/**
	 * Return a map of plugin ids.
	 * 
	 * @return
	 */
	Map<String, String> pluginIdsMap();

	/**
	 * Return a list of started mesh plugins.
	 * 
	 * @return
	 */
	List<MeshPlugin> getStartedMeshPlugins();

	/**
	 * Return the configured plugin startup and init timeout.
	 * 
	 * @return
	 */
	Duration getPluginTimeout();

}
