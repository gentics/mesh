package com.gentics.mesh.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.vertx.core.Vertx;

/**
 * Interface for a Gentics Mesh plugin. After deployment a plugin needs to register itself at the plugin manager.
 */
public interface MeshPlugin {

	/**
	 * Return the plugin manifest.
	 * 
	 * @return Manifest of the plugin
	 */
	PluginManifest getManifest();

	/**
	 * Method which can be used to initialize the plugin. The configured plugin timeout will be applied to the operation.
	 * 
	 * @return Completable which completes once the plugin has been initialized
	 */
	Completable initialize();

	/**
	 * Method which will be invoked once the the plugins has been de-registered. The plugin will be stopped afterwards. Use this method to free resources and
	 * stop processes that have been started or created during {@link #initialize()}. The configured plugin timeout will be applied to the operation.
	 * 
	 * @return Completable which completes the shutdown process.
	 */
	Completable shutdown();

	/**
	 * Return the Id of the plugin.
	 * 
	 * @return ID of the plugin
	 */
	String id();

	/**
	 * Shortcut for loading the name from the {@link #getManifest()}
	 * 
	 * @return Name of the plugin
	 */
	default String name() {
		return getManifest().getName();
	}

	/**
	 * Return the response which describes the plugin.
	 * 
	 * @return Plugin Response REST model
	 */
	default PluginResponse toResponse() {
		PluginResponse response = new PluginResponse();
		response.setManifest(getManifest());
		response.setName(name());
		response.setId(id());
		return response;
	}

	/**
	 * Return the storage location where plugin can persist data in the filesystem.
	 * 
	 * @return
	 */
	File getStorageDir();

	/**
	 * Write the provided config to the config file.
	 * 
	 * @param config
	 * @return
	 * @throws IOException
	 */
	<T> T writeConfig(T config) throws IOException;

	/**
	 * Return the plugin configuration.
	 * 
	 * @param clazz
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	<T> T readConfig(Class<T> clazz) throws FileNotFoundException, IOException;

	/**
	 * Return the rx java variant of Vert.x
	 * 
	 * @return RX Java variant Vert.x Instance
	 */
	default io.vertx.reactivex.core.Vertx getRxVertx() {
		return new io.vertx.reactivex.core.Vertx(vertx());
	}

	/**
	 * Return the plugin environment.
	 * 
	 * @return
	 */
	PluginEnvironment environment();

	/**
	 * Return the plugin admin client.
	 * 
	 * @return
	 */
	MeshRestClient adminClient();

	/**
	 * return the Vert.x instance that can be used for the plugin.
	 */
	Vertx vertx();
}
