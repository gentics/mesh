package com.gentics.mesh.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.plugin.PluginManifest;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.vertx.ext.web.Router;

/**
 * Interface for a Gentics Mesh plugin. After deployment a plugin needs to register itself at the plugin manager.
 */
public interface MeshPlugin {

	/**
	 * Shortcut for loading the name from the {@link #getManifest()}
	 * 
	 * @return Name of the plugin
	 */
	default String getName() {
		return getManifest().getName();
	}

	/**
	 * Shortcut for loading the api name from the {@link #getManifest()}
	 * 
	 * @return API name of the plugin
	 */
	default String getAPIName() {
		return getManifest().getApiName();
	}

	/**
	 * Return the plugin manifest.
	 * 
	 * @return Manifest of the plugin
	 */
	PluginManifest getManifest();

	/**
	 * Method which can be used to initialize the plugin.
	 * 
	 * @return Completable which completes once the plugin has been initialized
	 */
	Completable initialize();

	/**
	 * Method which will be invoked once the endpoints of the plugins have been de-registered and the plugin will be stopped.
	 * 
	 * @return
	 */
	Completable prepareStop();

	/**
	 * Return the deployment Id of the plugin.
	 * 
	 * @return Deployment ID of the plugin verticle
	 */
	String deploymentID();

	/**
	 * Return the response which describes the plugin.
	 * 
	 * @return Plugin Response REST model
	 */
	default PluginResponse toResponse() {
		PluginResponse response = new PluginResponse();
		response.setManifest(getManifest());
		response.setName(getName());
		response.setUuid(deploymentID());
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
		return new io.vertx.reactivex.core.Vertx(Mesh.vertx());
	}

}
