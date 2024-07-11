package com.gentics.mesh.impl;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshFactory;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.cli.MeshImpl;
import com.gentics.mesh.dagger.DaggerHibernateMeshComponent;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Factory which will create and maintain the state of a single mesh instance.
 */
public class HibernateMeshFactoryImpl implements MeshFactory {

	@Override
	public Mesh create() {
		return create(OptionsLoader.createOrloadOptions(HibernateMeshOptions.class));
	}

	@Override
	public Mesh create(MeshOptions options) {
		return new MeshImpl(options, DaggerHibernateMeshComponent.builder());
	}

}
