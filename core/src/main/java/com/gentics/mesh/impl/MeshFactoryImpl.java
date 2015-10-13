package com.gentics.mesh.impl;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshFactory;
import com.gentics.mesh.cli.MeshImpl;
import com.gentics.mesh.etc.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Factory which will create and maintain the state of a single mesh instance.
 *
 */
public class MeshFactoryImpl implements MeshFactory {

	private static Mesh instance;

	@Override
	public Mesh mesh() {
		if (instance == null) {
			instance = new MeshImpl(OptionsLoader.createOrloadOptions());
		}
		return instance;
	}

	@Override
	public Mesh mesh(MeshOptions options) {
		if (instance == null) {
			instance = new MeshImpl(options);
			return instance;
		} else {
			throw new RuntimeException("Instance is still active. Please shutdown any running instance of mesh first.");
		}
	}

	/**
	 * Clear the stored instance.
	 */
	public static void clear() {
		instance = null;
	}

}
