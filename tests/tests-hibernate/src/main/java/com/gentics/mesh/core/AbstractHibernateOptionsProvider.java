package com.gentics.mesh.core;

import com.fasterxml.jackson.databind.ObjectMapper;
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

	@Override
	public MeshOptions getClone() throws Exception {
		ObjectMapper mapper = OptionsLoader.getYAMLMapper();
		String optionsAsString = mapper.writeValueAsString(meshOptions);

		HibernateMeshOptions clonedMeshOptions = mapper.readValue(optionsAsString, HibernateMeshOptions.class);

		// by actually sharing the storage options, we make sure that if the database connection settings are changed in the original mesh options,
		// all instances will get the updated settings
		clonedMeshOptions.setStorageOptions(meshOptions.getStorageOptions());

		return clonedMeshOptions;
	}
}

