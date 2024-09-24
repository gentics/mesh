package com.gentics.mesh.core;

import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;

public abstract class AbstractHibernateOptionsProvider implements HibernateMeshOptionsProvider {

	protected final HibernateMeshOptions meshOptions;
	
	public AbstractHibernateOptionsProvider() {
		meshOptions = OptionsLoader.createOrloadOptions(HibernateMeshOptions.class, OptionsLoader.generateDefaultConfig(HibernateMeshOptions.class, null));
	}

	@Override
	public MeshOptions getOptions() {
		fillMeshOptions(meshOptions);
		return meshOptions;
	}
}

