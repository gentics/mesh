package com.gentics.mesh.plugin.test.common;

import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.test.local.MeshLocalServer;

/**
 * OrientDB specific extension to {@link MeshLocalServer}
 */
public class OrientDBMeshLocalServer extends MeshLocalServer {
	/**
	 * Create an instance
	 */
	public OrientDBMeshLocalServer() {
		super(OptionsLoader.createOrloadOptions(OrientDBMeshOptions.class));
	}

	/**
	 * Set the memory mode flag.
	 * 
	 * @return Fluent API
	 */
	public OrientDBMeshLocalServer withInMemoryMode() {
		OrientDBMeshOptions meshOptions = (OrientDBMeshOptions) getMeshOptions();
		meshOptions.getStorageOptions().setDirectory(null);
		return this;
	}

}
